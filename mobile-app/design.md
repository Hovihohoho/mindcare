# Design — MindCare Mobile

A locked design system for the MindCare consumer mobile app. Presentation changes
must preserve the existing routes, data contracts, authentication, assessment,
AI and Health Connect behavior.

## Genre

Modern-minimal: calm, trustworthy and human without reading as clinical.

## Macrostructure family

- App pages: Native Content Flow — page → section → content → action.
- Focus flows: Long Document — progress → prompt → response → action.
- Content pages: Index-First — compact rows separated by rules.

## Theme

- `--color-paper`: oklch(98.2% 0.006 174)
- `--color-paper-2`: oklch(100% 0 0)
- `--color-ink`: oklch(27% 0.032 190)
- `--color-ink-2`: oklch(53% 0.018 190)
- `--color-rule`: oklch(92.5% 0.009 174)
- `--color-accent`: oklch(55% 0.075 185)
- `--color-accent-ink`: oklch(100% 0 0)
- `--color-focus`: oklch(47% 0.074 185)

## Typography

- Display: Be Vietnam Pro, weight 600, normal.
- Body: Be Vietnam Pro, weight 400–500.
- Page title: 30px; section title: 20px; body: 15px.
- Use bold sparingly and never italicize headings.

## Spacing

4-point scale: 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40. Page gutters are 20px,
section rhythm is 28–32px, and touch targets are at least 44px.

## Motion

- Pressed feedback uses opacity and at most 1px translation.
- No decorative entrance animation.
- System reduced-motion preferences take precedence.

## Microinteractions stance

- Silent success with inline status.
- Errors stay near the action and offer retry.
- Focus, selected, disabled and loading states remain explicit.

## CTA voice

- Primary: solid muted teal, 12px radius, direct verb-first copy.
- Secondary: white or transparent surface with a quiet rule.

## Per-page allowances

- App pages do not use enrichment; function carries the page.
- Shadows are reserved for modal and bottom-sheet elevation.
- Cards are used only for bounded input or elevated content.

## What pages MUST share

- Muted teal accent and warm white canvas.
- Be Vietnam Pro typography.
- 20px page gutter, compact headers and quiet dividers.
- White system bottom navigation with a hairline top rule.

## What pages MAY differ on

- Chat may pin its composer while list screens scroll naturally.
- Assessment focus flows may use bounded answer options.
- Semantic colors may appear only for health, emotion and status data.

## Exports

The canonical CSS export is in `tokens.css`; React Native consumes the matching
values from `src/theme/tokens.ts`.
