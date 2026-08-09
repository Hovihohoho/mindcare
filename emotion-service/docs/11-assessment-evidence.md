# Assessment evidence contract

## Publication gate

An assessment may only be published when all three registries support its code:

1. structural definition and response scale;
2. versioned scoring policy;
3. traceable evidence metadata.

Public assessment summary and detail responses expose the publisher, source title,
source URL, publication year, instrument version, licence, scoring rule version,
purpose and limitation. The web client displays the source before the user starts.

## WHO-5

- Primary source: World Health Organization, *The World Health
  Organization-Five Well-Being Index (WHO-5)*, 2024.
- URL: https://www.who.int/publications/m/item/WHO-UCN-MSD-MHE-2024.01
- Licence: CC BY-NC-SA 3.0 IGO.
- Structure: five statements about the previous two weeks, each scored 0..5;
  raw total 0..25, where a higher score represents better well-being.

The application does not create several unsupported severity bands for WHO-5.
Its current support threshold separates raw scores 0..12 from 13..25 and always
states that the result is self-assessment/support information, not a diagnosis.
Any future threshold or wording change requires a new scoring-rule version,
source review and regression tests.
