// Run with node scripts/notification-check.cjs. Uses the installed TypeScript compiler;
// native rendering is stubbed, while screen handlers, API client and auth provider run unchanged.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const root = path.resolve(__dirname, '../src');
const cache = new Map();
let host;
const changed = (before, after) => !before || before.length !== after.length || before.some((value, index) => value !== after[index]);
const react = {
  createContext: (value) => ({ Provider: 'Provider', value }),
  useContext: (context) => context.value,
  useState(initial) {
    const owner = host, index = owner.index++;
    if (!(index in owner.slots)) owner.slots[index] = typeof initial === 'function' ? initial() : initial;
    return [owner.slots[index], (value) => { owner.slots[index] = typeof value === 'function' ? value(owner.slots[index]) : value; }];
  },
  useRef(initial) { const owner = host, index = owner.index++; return owner.slots[index] ??= { current: initial }; },
  useMemo(factory, dependencies) {
    const owner = host, index = owner.index++;
    if (!owner.slots[index] || changed(owner.slots[index].dependencies, dependencies)) owner.slots[index] = { dependencies, value: factory() };
    return owner.slots[index].value;
  },
  useCallback(callback, dependencies) { return react.useMemo(() => callback, dependencies); },
  useEffect(callback, dependencies) {
    const owner = host, index = owner.index++;
    const previous = owner.slots[index];
    if (!previous || changed(previous.dependencies, dependencies)) {
      owner.effects.push(() => { previous?.cleanup?.(); owner.slots[index] = { dependencies, cleanup: callback() }; });
    }
  },
};
const jsx = (type, props) => ({ type, props });
const auth = { session: { accessToken: 'test-token' }, status: 'authenticated' };
const counts = { unreadCount: 2, revision: 0, setCount: (count) => { counts.unreadCount = count; } };
const navigation = [];
const alerts = [];
let cleared = false;
const native = Object.fromEntries(['Alert', 'FlatList', 'Pressable', 'RefreshControl', 'Text', 'View'].map((name) => [name, name]));
native.Alert = { alert: (...args) => alerts.push(args) };
native.StyleSheet = { create: (styles) => styles, hairlineWidth: 1 };
const mocks = {
  react, 'react/jsx-runtime': { jsx, jsxs: jsx },
  'expo-router': { router: { navigate: (href) => navigation.push(href), back() {} }, Redirect: 'Redirect', useFocusEffect: (callback) => react.useEffect(callback, [callback]) },
  'react-native': native,
  '@/features/auth/auth-context': { useAuth: () => auth },
  '@/features/notifications/notification-context': { useNotifications: () => counts },
  '@/services/auth/session.storage': { sessionStorage: { load: async () => auth.session, save: async () => {}, clear: async () => { cleared = true; } } },
  '@/services/auth/auth.service': { authService: { me: async () => ({ id: 'user' }) } },
  '@/theme/tokens': { colors: {}, fonts: {}, spacing: {}, type: {} },
};
for (const [file, names] of Object.entries({ 'app-screen': ['AppScreen'], 'screen-header': ['ScreenHeader'], 'data-states': ['DataFeedback', 'SkeletonList'] })) {
  mocks[`@/components/${file}`] = Object.fromEntries(names.map((name) => [name, name]));
}
function load(name, from = root) {
  if (mocks[name]) return mocks[name];
  let file = name.startsWith('@/') ? path.join(root, name.slice(2)) : path.resolve(from, name);
  if (file.endsWith('api.config')) return { API_URL: 'http://notification-test.invalid' };
  file = [file, `${file}.ts`, `${file}.tsx`].find((candidate) => fs.existsSync(candidate));
  assert.ok(file, `Module exists: ${name}`);
  if (cache.has(file)) return cache.get(file).exports;
  const module = { exports: {} }; cache.set(file, module);
  const source = ts.transpileModule(fs.readFileSync(file, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, target: ts.ScriptTarget.ES2022 } }).outputText;
  new Function('require', 'module', 'exports', source)((dependency) => load(dependency, path.dirname(file)), module, module.exports);
  return module.exports;
}
function mount(component) {
  const owner = { slots: [], effects: [], index: 0 };
  owner.render = () => { host = owner; owner.index = 0; const result = component({ children: null }); owner.effects.splice(0).forEach((effect) => effect()); return result; };
  owner.dispose = () => owner.slots.forEach((slot) => slot?.cleanup?.());
  return owner;
}
function nodes(tree, type) {
  if (!tree || typeof tree !== 'object') return [];
  if (Array.isArray(tree)) return tree.flatMap((node) => nodes(node, type));
  return [...(tree.type === type ? [tree] : []), ...nodes(tree.props?.children, type)];
}
const tick = () => new Promise((resolve) => setImmediate(resolve));
const item = { id: 'notification-1', title: 'Check-in', message: 'Nội dung', actionUrl: '/emotion', readAt: null, createdAt: '2026-10-04T10:00:00Z' };
let response = { items: [item], page: 0, size: 20, totalElements: 1, totalPages: 1, unreadCount: 2 };
let status = 200;
const requests = [];
global.fetch = async (url, options) => {
  requests.push({ url, options });
  return { status, ok: status < 400, json: async () => ({ success: status < 400, message: 'Test error', data: options.method === 'PATCH' ? null : response }) };
};

(async () => {
  const Screen = load('@/screens/notification-inbox-screen').default;
  const screen = mount(Screen);
  assert.equal(nodes(screen.render(), 'SkeletonList').length, 0);
  assert.equal(nodes(screen.render(), 'SkeletonList').length, 1);
  await tick();
  let tree = screen.render();
  let list = nodes(tree, 'FlatList')[0];
  assert.equal(list.props.data.length, 1);
  assert.equal(counts.unreadCount, 2);
  assert.equal(requests[0].options.headers.Authorization, 'Bearer test-token');
  await list.props.renderItem({ item }).props.onPress(); await tick();
  list = nodes(screen.render(), 'FlatList')[0];
  assert.ok(list.props.data[0].readAt);
  assert.equal(counts.unreadCount, 1);
  assert.deepEqual(navigation[0], { pathname: '/(tabs)/journal', params: { checkIn: 'true' } });
  assert.match(requests.at(-1).url, /notifications\/notification-1\/read$/);
  assert.equal(requests.at(-1).options.method, 'PATCH');
  status = 500;
  await list.props.renderItem({ item: { ...item, id: 'failed-read' } }).props.onPress(); await tick();
  assert.equal(counts.unreadCount, 1);
  assert.equal(navigation.length, 1);
  assert.equal(nodes(screen.render(), 'DataFeedback')[0].props.kind, 'error');
  status = 200;
  const all = nodes(screen.render(), 'Pressable')[0];
  await all.props.onPress(); await tick();
  assert.equal(counts.unreadCount, 0);
  assert.equal(nodes(screen.render(), 'Pressable').length, 0);
  assert.match(requests.at(-1).url, /notifications\/read-all$/);

  const unknown = { ...item, id: 'unknown', actionUrl: 'https://evil.invalid' };
  await list.props.renderItem({ item: unknown }).props.onPress(); await tick();
  assert.equal(navigation.length, 1);
  assert.equal(alerts.length, 1);
  response = { ...response, items: [item], totalPages: 2 };
  list.props.refreshControl.props.onRefresh(); await tick();
  list = nodes(screen.render(), 'FlatList')[0];
  response = { ...response, page: 1, items: [{ ...item, id: 'notification-2' }] };
  list.props.ListFooterComponent.props.onPress(); await tick();
  list = nodes(screen.render(), 'FlatList')[0];
  assert.equal(list.props.data.length, 2);
  assert.match(requests.at(-1).url, /page=1&size=20$/);
  response = { ...response, items: [], unreadCount: 0 };
  list.props.refreshControl.props.onRefresh(); await tick();
  tree = screen.render(); list = nodes(tree, 'FlatList')[0];
  assert.equal(list.props.ListEmptyComponent.props.kind, 'empty');
  status = 500;
  list.props.refreshControl.props.onRefresh(); await tick();
  tree = screen.render();
  assert.equal(nodes(tree, 'DataFeedback')[0].props.kind, 'error');
  assert.equal(nodes(tree, 'FlatList')[0].props.ListEmptyComponent, null);
  screen.dispose();

  const destination = load('@/features/notifications/notification-navigation').notificationDestination;
  for (const invalid of [null, {}, '', '//evil.invalid', '/missing', '/(auth)/login', '/journal/../settings']) assert.equal(destination(invalid), null);
  assert.equal(destination('/care-plan'), '/(tabs)/progress');
  assert.equal(destination('/health-connect'), '/(tabs)/health-connect');

  // Check event-driven badge refresh and subscription cleanup, without native push registration.
  let foreground, received, removed = 0;
  native.AppState = { addEventListener: (_event, callback) => { foreground = callback; return { remove: () => removed++ }; } };
  mocks['./push-registration'] = { canUseLocalNotifications: () => true, loadNotifications: async () => ({ addNotificationReceivedListener: (callback) => { received = callback; return { remove: () => removed++ }; } }) };
  delete mocks['@/features/notifications/notification-context'];
  const badge = mount(load('@/features/notifications/notification-context').NotificationProvider);
  status = 200; response = 3;
  badge.render(); await tick();
  assert.equal(badge.render().props.value.unreadCount, 3);
  assert.match(requests.at(-1).url, /api\/auth\/notifications\/unread-count$/);
  response = 4; received(); await tick();
  assert.equal(badge.render().props.value.unreadCount, 4);
  response = 5; foreground('active'); await tick();
  assert.equal(badge.render().props.value.unreadCount, 5);
  status = 500; foreground('active'); await tick();
  assert.equal(badge.render().props.value.unreadCount, null);
  badge.dispose(); assert.equal(removed, 2);

  // Exercise the actual centralized 401 subscriber and AuthProvider storage/session clearing.
  delete mocks['@/features/auth/auth-context'];
  const provider = mount(load('@/features/auth/auth-context').AuthProvider);
  status = 200; provider.render(); await tick();
  assert.equal(provider.render().props.value.status, 'authenticated');
  status = 401;
  const service = load('@/services/notifications/notification.service').notificationService;
  await assert.rejects(service.list('test-token'), (error) => error.status === 401);
  await tick();
  assert.equal(provider.render().props.value.session, null);
  assert.equal(provider.render().props.value.status, 'unauthenticated');
  assert.ok(cleared);
  provider.dispose();
  console.log('PASS — inbox loading/list/pagination/empty/error, unread count, read/read-all UI and mutation failure, safe navigation, foreground/push refresh, centralized 401 session clearing');
})().catch((error) => { console.error(error); process.exitCode = 1; });
