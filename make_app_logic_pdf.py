"""Regenerates APP_LOGIC.pdf - the branded, typeset code guide.

Run:  python3 make_app_logic_pdf.py
"""
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (BaseDocTemplate, Frame, PageTemplate,
                                Paragraph, PageBreak, Spacer, Table,
                                TableStyle, Preformatted)
from reportlab.platypus.tableofcontents import TableOfContents

PURPLE = colors.HexColor("#6A35FF")
DARK = colors.HexColor("#14121D")
GREY = colors.HexColor("#6D6B73")
GREEN = colors.HexColor("#12B76A")
ORANGE = colors.HexColor("#F5A623")
LAV = colors.HexColor("#F0ECFB")
LIGHTGREY = colors.HexColor("#F6F5FA")
WHITE = colors.white

W, H = A4

TITLE = ParagraphStyle("title", fontName="Helvetica-Bold", fontSize=26,
                       textColor=WHITE, leading=30)
SUBTITLE = ParagraphStyle("subtitle", fontName="Helvetica", fontSize=11,
                          textColor=LAV, leading=14)
H1 = ParagraphStyle("h1", fontName="Helvetica-Bold", fontSize=15,
                    textColor=PURPLE, spaceBefore=14, spaceAfter=6,
                    leading=18)
H2 = ParagraphStyle("h2", fontName="Helvetica-Bold", fontSize=11.5,
                    textColor=DARK, spaceBefore=10, spaceAfter=4, leading=14)
BODY = ParagraphStyle("body", fontName="Helvetica", fontSize=9.5,
                      textColor=DARK, leading=13, spaceAfter=4)
BULLET = ParagraphStyle("bullet", parent=BODY, leftIndent=12,
                        bulletIndent=2, spaceAfter=2)
CODE = ParagraphStyle("code", fontName="Courier", fontSize=7.8,
                      textColor=DARK, leading=9.6, backColor=LIGHTGREY,
                      borderPadding=(4, 6, 4, 6), spaceAfter=6)
TOC_H = ParagraphStyle("toch", fontName="Helvetica-Bold", fontSize=13,
                       textColor=DARK, spaceAfter=8)


class DocTemplate(BaseDocTemplate):
    def __init__(self, filename):
        frame = Frame(16 * mm, 16 * mm, W - 32 * mm, H - 32 * mm,
                      id="main")
        super().__init__(filename, pagesize=A4, title="PalmPay Clone - Complete Code Guide")
        self.addPageTemplates([PageTemplate(id="all", frames=[frame],
                                            onPage=self._chrome)])
        self.toc = TableOfContents()
        self.toc.levelStyles = [
            ParagraphStyle("toc0", fontName="Helvetica-Bold", fontSize=10,
                           textColor=PURPLE, spaceAfter=4, leading=12),
            ParagraphStyle("toc1", fontName="Helvetica", fontSize=9,
                           textColor=GREY, leftIndent=14, spaceAfter=2,
                           leading=11),
        ]

    def _chrome(self, canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(PURPLE)
        canvas.setLineWidth(0.8)
        canvas.line(16 * mm, H - 12 * mm, W - 16 * mm, H - 12 * mm)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.setFillColor(PURPLE)
        canvas.drawString(16 * mm, H - 10 * mm, "PALMPAY CLONE")
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(GREY)
        canvas.drawRightString(W - 16 * mm, H - 10 * mm,
                               "Complete Code Guide")
        canvas.setStrokeColor(LAV)
        canvas.line(16 * mm, 12 * mm, W - 16 * mm, 12 * mm)
        canvas.drawRightString(W - 16 * mm, 9 * mm, "page %d" % doc.page)
        canvas.restoreState()

    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph):
            if flowable.style.name == "h1":
                self.notify("TOCEntry", (0, flowable.getPlainText(),
                                         self.page))
            elif flowable.style.name == "h2":
                self.notify("TOCEntry", (1, flowable.getPlainText(),
                                         self.page))


def bullet(text):
    return Paragraph("•  " + escape(text), BULLET)


def code(text):
    return Preformatted(text, CODE)


def kv_table(rows):
    data = [[Paragraph("<b>%s</b>" % escape(a),
                       ParagraphStyle("ka", fontName="Helvetica-Bold",
                                      fontSize=8.6, textColor=PURPLE,
                                      leading=11)),
             Paragraph(escape(b),
                       ParagraphStyle("kb", fontName="Courier",
                                      fontSize=8, textColor=DARK,
                                      leading=10))]
            for a, b in rows]
    t = Table(data, colWidths=[62 * mm, W - 32 * mm - 66 * mm],
              hAlign="LEFT")
    style = [("VALIGN", (0, 0), (-1, -1), "TOP"),
             ("TOPPADDING", (0, 0), (-1, -1), 4),
             ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
             ("LEFTPADDING", (0, 0), (-1, -1), 6),
             ("GRID", (0, 0), (-1, -1), 0.4, LAV),
             ("BACKGROUND", (0, 0), (0, -1), LAV)]
    for i in range(0, len(data)):
        if i % 2 == 1:
            style.append(("BACKGROUND", (1, i), (1, i), LIGHTGREY))
    t.setStyle(TableStyle(style))
    return t


def build_story(doc):
    s = []
    # ---- cover block -------------------------------------------------
    cover = Table(
        [[Paragraph("PALMPAY CLONE", ParagraphStyle(
              "k", fontName="Helvetica-Bold", fontSize=12, textColor=LAV,
              leading=14))],
         [Paragraph("Complete Code Guide", TITLE)],
         [Paragraph("Every screen, class and flow in this repository, "
                    "explained in plain terms - with a where-to-edit "
                    "cheat sheet. Java + XML, MVC-style controllers, "
                    "immutable models, repository data sources.",
                    SUBTITLE)]],
        colWidths=[W - 32 * mm])
    cover.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), PURPLE),
        ("TOPPADDING", (0, 0), (0, 0), 14),
        ("BOTTOMPADDING", (0, 0), (0, 0), 14),
        ("LEFTPADDING", (0, 0), (0, 0), 12),
    ]))
    s += [cover, Spacer(1, 10),
          Paragraph("Contents", TOC_H), doc.toc, PageBreak()]

    def h1(t):
        s.append(Paragraph(escape(t), H1))

    def h2(t):
        s.append(Paragraph(escape(t), H2))

    def p(t):
        s.append(Paragraph(escape(t), BODY))

    # ---- 0 -----------------------------------------------------------
    h1("0. Big picture")
    p("The app is Java + XML (no Kotlin, no Compose). Each screen has an "
      "Activity (the shell) plus a Controller (all behaviour). Data lives "
      "in repositories and stores, and screens render immutable model "
      "objects built by catalogues - so replacing demo data with a real "
      "API later only touches the data classes.")
    s.append(kv_table([
        (".MainActivity", "Home screen shell: view binding, system bars."),
        (".ui.HomeScreenController", "All home behaviour and carousels."),
        (".data.WalletStore", "SharedPreferences: balance, name, key."),
        (".data.HomeCatalog", "Static home content as models."),
        (".model.*", "QuickAction, ServiceAction, PromotionCard."),
        (".profile.*", "ProfileActivity + controller (customisation)."),
        (".transfer.ui.*", "Transfer, Amount, BankPicker screens."),
        (".transfer.data.*", "Paystack, Stripe, Flutterwave, NUBAN, logos, names."),
        (".transfer.model.*", "BankInstitution, TransferRecipient, ..."),
    ]))
    p("Layouts live in res/layout (one XML per screen and per row); "
      "light/dark values in res/values and res/values-night; XML shapes "
      "in res/drawable and PNG art in res/drawable-nodpi.")

    # ---- 1 -----------------------------------------------------------
    h1("1. Home screen")
    p("MainActivity.onCreate builds its ViewBinding and calls "
      "HomeScreenController.bind(), which renders each region in order:")
    for t in [
        "renderQuickActions() - To Bank / To PalmPay / Savings / Cards; To Bank opens TransferActivity.",
        "renderServices() - the 8-tile grid; the Data tile carries the orange Promo badge.",
        "renderPromotions() - Team Up & Save and CashBox cards.",
        "bindBalanceCard() - WalletStore balance with hide/show eye; Add Money / History are toasts.",
        "bindHeader() - greeting 'Hi, <name>' from WalletStore; avatar opens Profile.",
        "bindPromoCarousel() / bindBannerCarousel() - ViewFlippers auto-flipping every 4000 ms with fades; dots synced by a Handler loop.",
        "bindNavigation() - custom bottom bar (icon masks tinted purple/ink, red dot on Loan, NEW pill on Wealth).",
    ]:
        s.append(bullet(t))
    h2("Where to edit")
    for t in [
        "Tile texts: res/values/strings.xml; which tiles exist: data/HomeCatalog.java.",
        "Tap behaviour: ui/HomeScreenController.java; carousel speed: the setFlipInterval(4000) calls.",
        "Colours per theme: res/values/colors.xml + res/values-night/colors.xml.",
    ]:
        s.append(bullet(t))

    # ---- 2 -----------------------------------------------------------
    h1("2. Profile (customisation form)")
    p("One Save button persists every field into the "
      "'palmpay_clone_wallet' SharedPreferences, and Home re-reads the "
      "balance in onResume().")
    s.append(kv_table([
        ("Balance", "saveBalance() - the number the home card shows."),
        ("Display Name", "saveDisplayName() - drives the home greeting."),
        ("Paystack key", "savePaystackApiKey() - name lookup + Add Money route 1."),
        ("Stripe key", "saveStripeApiKey() - Add Money route 2."),
        ("Flutterwave key", "saveFlutterwaveApiKey() - Add Money route 3."),
        ("Flutterwave enc key", "saveFlutterwaveEncKey() - blank = derived."),
        ("Email", "savePaystackEmail() - customer on every card charge."),
    ]))
    h2("Where to edit")
    s.append(bullet("Add a field: activity_profile.xml + saveAll() in ProfileScreenController."))

    # ---- 3 -----------------------------------------------------------
    h1("3. Transfer to Bank")
    p("TransferActivity is the shell (100 ms centre-expand enter "
      "animation, result routing). TransferScreenController owns all "
      "logic.")
    h2("3.1 Account input")
    for t in [
        "A TextWatcher formats digits as '911 241 3798' and calls updateAccountMatch(digits) after every change.",
        "< 10 digits: history suggestions with progressive purple prefix (SpannableString + ForegroundColorSpan). Any edit clears ALL verification state.",
        "== 10 digits: the matching-banks request cycle below.",
    ]:
        s.append(bullet(t))
    h2("3.2 Matching banks request cycle - showMatchingBanks()")
    for t in [
        "Clears the container, shows the 'Matching banks' spinner row.",
        "History banks for this account are added instantly with their saved holder names.",
        "With a key: PaystackClient.listBanks() (memory -> disk cache -> network) gives the general directory; targets = wallets (opay, palmpay, moniepoint, smartcash, kuda, momo) + up to MAX_VERIFIED_PROBES (10) NUBAN-valid banks.",
        "One resolveAccount() per target, ALL CONCURRENTLY (OkHttp dispatcher 64 / 40 per host). Successes stream in as rows; failures are remembered in failedBankKeys; the last settle hides the spinner.",
        "Without a key: offline NUBAN candidates from the GitHub-pages directory, no names.",
    ]:
        s.append(bullet(t))
    p("Policy (user requirement): verification results are NEVER "
      "persisted. Deleting and retyping a digit always re-runs the whole "
      "request; only the general bank list and logos are cached on "
      "device.")
    h2("3.3 Selecting a bank")
    for t in [
        "selectMatchedBank()/selectBank(): name in resolvedNames -> instant; known-failed -> red 'Invalid account' banner; otherwise 'Verifying account name' spinner + resolveNameViaPaystack().",
        "resolveNameViaPaystack(): GET /bank/resolve with account_number & bank_code (snake_case, per Paystack); success -> confirmation row + Next; failure -> banner.",
        "Next gating: isFormReady() = 10 digits AND bank selected AND a resolved name; a disabled Next is silent.",
    ]:
        s.append(bullet(t))
    h2("3.4 Support classes")
    s.append(kv_table([
        ("NubanBankResolver", "Static CBN check-digit math (3-7-3 weights, mod 10)."),
        ("BankNameNormalizer", "Suffix stripping, aliases, dedupe by code/canonical name."),
        ("BankLogoResolver", "Canonical name -> bundled rounded logo resource."),
        ("BankLogoLoader", "Downloads logos, circle-clips ONCE, memory + disk cache."),
        ("BankDirectoryRepository", "Banks.json fetch + offline fallback, deduped."),
        ("PaystackClient", "OkHttp; cached general list; resolve; no result persistence."),
    ]))

    # ---- 4 -----------------------------------------------------------
    h1("4. Bank picker")
    for t in [
        "'Frequently Used Bank' grid: verified NUBAN matches for the passed account top it; otherwise a fixed set.",
        "Alphabetical full list from the directory with section headers and an A-Z rail; search filters by name/code.",
        "Logos via BankLogoLoader; selection returns to selectBank().",
    ]:
        s.append(bullet(t))

    # ---- 5 -----------------------------------------------------------
    h1("5. Amount screen")
    for t in [
        "Custom keypad (GridLayout amount_keypad); amount field suppresses the system keyboard; Note field shows Gboard and hides the keypad (focus choreography); manifest uses adjustResize.",
        "Live comma grouping (formatAmount); parseAmount strips commas.",
        "Validation 10.00-200,000.00 with red amount_error and disabled Next; >= 10,000 shows the FIRS stamp-duty notice (13sp).",
        "Balance line 'Balance: N 0.00  CashBox: <wallet>' (Balance is always 0 by product rule).",
    ]:
        s.append(bullet(t))
    h2("Where to edit")
    s.append(bullet("Rules: AmountScreenController constants MIN_AMOUNT / MAX_AMOUNT / STAMP_DUTY_THRESHOLD; keys in activity_amount.xml."))

    # ---- 6 -----------------------------------------------------------
    h1("6. Add Money (card funding)")
    p("AddMoneyActivity is the shell (it also owns the picture-in-picture "
      "behaviour); AddMoneyController owns the whole flow. It collects an "
      "amount, VISA number, expiry, CVV and the PalmPay phone number, "
      "charges the card, then forwards the money to the wallet.")
    h2("6.1 The silent destination gate")
    for t in [
        "ALLOWED_SUFFIX 4043: any other number never reaches a processor - it fails after GATE_DELAY_MS (1.4 s) of fake processing with the generic am_generic_fail text and nothing hints at the rule.",
        "A matching number continues to the selected route, and every other failure surfaces the processor's REAL message.",
    ]:
        s.append(bullet(t))
    h2("6.2 Route 1 - Paystack (default)")
    p("POST /charge feeds the handleStep() state machine: success leads to "
      "transfer() (POST /transfer to bank code 999991); open_url opens the "
      "browser and pollPending(); send_otp / send_pin / send_phone show the "
      "challenge row and then submitOtp/Pin/Phone(reference); pending polls "
      "every POLL_INTERVAL_MS; paused with an authorization_url opens the "
      "browser and polls. messageOf() appends am_card_declined_hint when "
      "Paystack reports that the card could not be processed or to contact "
      "the merchant, which is an issuer decline.")
    h2("6.3 Route 2 - Stripe")
    p("International card-not-present with CVV only: POST "
      "/v1/payment_intents with confirm=true and "
      "payment_method_data[card]. succeeded means charged (it settles to "
      "the linked bank); requires_action opens next_action.url in the "
      "browser and pollStripe() polls the PaymentIntent; "
      "requires_payment_method is an issuer decline plus the hint.")
    h2("6.4 Route 3 - Flutterwave")
    for t in [
        "The payload must be 3DES-encrypted: requestBody() wraps a single client field holding the Base64 ciphertext, keyed with the dashboard encryption key or the documented derivation (first 12 chars of the secret key plus the last 12 of its MD5) when that field is blank.",
        "POST /v3/charges?type=card, then meta.authorization.mode decides: redirect opens the browser and polls; otp posts to /v3/validate-charge; pin re-sends the SAME charge with authorization.pin.",
        "pollFlw() polls /v3/transactions/{id}/verify until the state is successful, then transfer() runs as usual.",
    ]:
        s.append(bullet(t))
    h2("6.5 Browser handling on low-RAM devices")
    p("enterPopupMode() puts this app into picture-in-picture first, the "
      "bank page opens in Chrome as its own full-screen task "
      "(FLAG_ACTIVITY_NEW_TASK) and moveTaskToBack(true) tucks the app "
      "away, so the issuer page is never killed together with this app. "
      "onResume() calls resumePending(), which polls the right route again "
      "using currentRoute. Every terminal state runs terminal(): a coloured "
      "result box plus an AlertDialog.")
    h2("Where to edit")
    for t in [
        "Gate: ALLOWED_SUFFIX; polling: POLL_ATTEMPTS / POLL_INTERVAL_MS in AddMoneyController.",
        "New route: a radio button in activity_add_money.xml, a client in transfer/data, a branch in onPay().",
    ]:
        s.append(bullet(t))

    # ---- 7 -----------------------------------------------------------
    h1("7. Persistence policy")
    s.append(kv_table([
        ("wallet prefs", "available_balance, display_name, paystack_api_key, stripe_api_key, flutterwave_api_key, flutterwave_enc_key, paystack_email"),
        ("paystack prefs", "bank_directory_json (GENERAL list only)"),
        ("cache dir", "bank_logos/ processed rounded logos"),
        ("never stored", "resolved names, failed banks, matching lists"),
    ]))

    # ---- 7 -----------------------------------------------------------
    h1("8. Themes")
    p("res/values and res/values-night define the same colour names and "
      "Android picks per system theme; drawable-night/ overrides art. "
      "Screens only reference @color/..., so a theme change is a single "
      "value edit.")

    # ---- 8 -----------------------------------------------------------
    h1("9. Tests & CI")
    for t in [
        "Catalogue/model unit tests guard invariants; NubanBankResolverTest uses the CBN circular's own examples.",
        "FlutterwaveClientTest checks the 3DES key rule and the encrypted {\"client\": ...} body round-trip in pure Java.",
        "BottomNavigationLabelTest (Robolectric) inflates MainActivity and asserts the five labels render.",
        "PaystackLiveProbeTest is an @Ignore-d live diagnostic.",
        "CI (.github/workflows/android.yml, user-supplied): unit tests, assembleDebug, APK upload on every push.",
    ]:
        s.append(bullet(t))

    # ---- 9 -----------------------------------------------------------
    h1("10. Cheat sheet - where do I change...?")
    s.append(kv_table([
        ("Name / balance / key storage", "data/WalletStore.java"),
        ("Home tiles & icons", "data/HomeCatalog.java"),
        ("Home behaviour / carousels", "ui/HomeScreenController.java"),
        ("Bottom bar look", "layout/bottom_nav_item.xml + bindNavigation()"),
        ("Transfer flow", "transfer/ui/TransferScreenController.java"),
        ("Matching-bank policy", "showMatchingBanks(), WALLET_PROVIDERS, MAX_VERIFIED_PROBES"),
        ("Name resolution HTTP", "transfer/data/PaystackClient.java"),
        ("Bank math", "transfer/data/NubanBankResolver.java"),
        ("Name variants / dupes", "transfer/data/BankNameNormalizer.java"),
        ("Logos", "BankLogoLoader/Resolver + drawable-nodpi/"),
        ("Amount rules", "transfer/ui/AmountScreenController.java"),
        ("Picker", "transfer/ui/BankPickerScreenController.java"),
        ("Add Money flow / gate", "transfer/ui/AddMoneyController.java"),
        ("Card routes", "transfer/data/{Paystack,Stripe,Flutterwave}Client.java"),
        ("Browser + pop-up", "transfer/ui/AddMoneyActivity.java"),
        ("Any text", "res/values/strings.xml"),
        ("Any colour", "res/values/colors.xml (+ values-night)"),
        ("Any shape / rounded bg", "res/drawable/*.xml"),
    ]))
    return s


def main():
    doc = DocTemplate("APP_LOGIC.pdf")
    doc.multiBuild(build_story(doc))
    print("APP_LOGIC.pdf generated")


if __name__ == "__main__":
    main()
