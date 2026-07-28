export function UserFooter() {
  return (
    <footer className="border-t border-line bg-white">
      <div className="page-container flex flex-col justify-between gap-8 py-12 md:flex-row md:items-center">
        <div>
          <p className="text-2xl font-bold text-brand-700">MindCare</p>
          <p className="mt-3 text-sm text-muted">MindCare. Đồng hành cùng tâm hồn Việt.</p>
        </div>
        <nav className="flex flex-wrap gap-x-8 gap-y-3 text-sm font-medium text-slate-600">
          <a href="#about">Giới thiệu</a>
          <a href="#terms">Điều khoản sử dụng</a>
          <a href="#privacy">Chính sách bảo mật</a>
          <a href="#contact">Liên hệ</a>
        </nav>
      </div>
    </footer>
  );
}
