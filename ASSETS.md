# Visual asset sources

This project keeps the visual assets local so the app works offline and does not depend on a remote image host.

- The utility and navigation glyphs are Android Vector Drawable adaptations of the open-source [Material Design Icons](https://github.com/google/material-design-icons) collection. The collection is distributed under the Apache License 2.0.
- The card and tile backgrounds are native Android shape drawables defined in `app/src/main/res/drawable`, rather than raster images. That keeps their corners and spacing crisp at every screen density.
- `avatar_reference.jpg`, `claim_reference.png`, and `gift_reference.png` are density-neutral crops from the user-supplied reference screenshot in `screenshots/`. The claim and gift crops use transparent masks so they also blend correctly in the dark theme. They are used only where the reference contains photographic or promotional artwork that a vector cannot reproduce exactly.
- `ic_cashbox.xml` and `ic_fixed_savings.xml` are small, original vector illustrations created for this clone and are not copied from the PalmPay application.
- The screen content and naming are based on the public PalmPay product reference at [palmpay.com](https://palmpay.com/). This repository does not bundle PalmPay-owned logos, photographs, or proprietary artwork.
- The bank picker loads the broad Nigerian bank/logo directory from the public [Nigeria Banks Logo API](https://github.com/jsanwo64/Nigeria-Banks-Logo-API), using its JSON endpoint at `https://jsanwo64.github.io/Nigeria-Banks-Logo-API/Banks.json`. It falls back to a local directory when offline, and only accepts HTTPS logo URLs.

The implementation is intentionally Java + XML: the XML layouts define the reusable visual components, while `HomeCatalog` supplies immutable models and `HomeScreenController` binds them and handles interactions.
