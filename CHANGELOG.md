# Changelog

## Unreleased

### Changed
- Remove `SecsDataItem.toFormatString()`; canonical SML formatting is now provided by `SmlPrinter.DEFAULT.toSml(...)`.  
  Migration: replace `sdi.toFormatString()` with `SmlPrinter.DEFAULT.toSml(sdi)` (or `toMachineSml` for the compact form).

### Fixes
- Parser: preserve unsigned semantics for `U1`/`U2` tokens during SML → SECS-II parsing (regression test added).

---

## 1.0.0 - (unreleased)
- See `Unreleased` for recent refactor and protocol hardening work.
