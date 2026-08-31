# Help translate Sente

[![Crowdin](https://badges.crowdin.net/sente/localized.svg)](https://crowdin.com/project/sente)

Sente is an Android app for playing Go (baduk, weiqi, 囲碁, 바둑) against players from around the
world. It's a hobby project, made in spare time, and it's free.

Right now the app speaks English and Romanian. If you'd like to see it in your language, this page
is for you. **You don't need to be a programmer, install anything, or know how Android works.** You
just need to speak the language and know a bit about Go.

## The short version

1. Go to **[crowdin.com/project/sente](https://crowdin.com/project/sente)**
2. Create a free Crowdin account (or sign in with GitHub/Google)
3. Pick your language
4. Start translating

That's it. Your translations are picked up automatically and will show up in a future release of the
app.

## Translating, step by step

### 1. Open the project

Head to **[crowdin.com/project/sente](https://crowdin.com/project/sente)** and sign in. Crowdin is a
free online tool for translating apps — everything happens in your browser.

### 2. Pick your language

You'll see a list of languages with a progress bar next to each one. Click the one you want to work
on. If your language isn't listed, see [Adding a new language](#adding-a-new-language) below.

### 3. Translate

Click the file (`strings.xml`) to open the editor. You'll see:

- the **English text** on the left,
- a **box to type your translation** in the middle,
- **suggestions** from machine translation and from other translators below it.

Type your translation, press **Save**, and Crowdin moves you to the next string. Repeat as long as
you feel like it. There's no commitment — translating five strings is genuinely useful, and you can
stop and come back whenever.

If you're unsure about a string, translate the ones you *are* sure about and skip it. Someone else
may pick it up, or you can leave a comment (see below).

### 4. That's it

There is no pull request to make, no file to send anyone. Translations are pulled from Crowdin into
the app's code automatically.

## Things worth knowing

### Placeholders like `%1$s` and `%d`

Some strings contain placeholders — the app fills these in with real values when it runs:

| English                         | What it means                                       |
|---------------------------------|-----------------------------------------------------|
| `White's chance to win: %1$s%%` | `%1$s` becomes a number, `%%` is a literal `%` sign |
| `%d week`                       | `%d` becomes a number, e.g. "3 weeks"               |

**Keep every placeholder exactly as it is** — same spelling, same number. You *can* move it around
to wherever it belongs in your language's word order. Crowdin will warn you if you accidentally drop
one.

✅ `Șansa albului de a câștiga: %1$s%%`
❌ `Șansa albului de a câștiga: %1 s%` (placeholder was broken)

### Apostrophes

You'll notice English strings write apostrophes as `\'` — for example `Here\'s what I think`. That
backslash is required by Android. If your translation contains an apostrophe, write it the same way:
`\'`. Crowdin usually keeps this correct if you use the "copy source" button as a starting point.

### Plurals

Some entries have several forms (`one`, `few`, `other`, …). Crowdin shows you a tab for each form
your language actually needs — Romanian gets three, English gets two, Japanese gets one. Fill in the
tabs you're shown and ignore the rest.

### Go terminology

This is where a translator who plays Go really beats a generic one. Words like *atari*, *ko*,
*seki*, *joseki*, *dame*, *komi*, *byo-yomi*, *handicap*, *territory* and *rank/rating* often have
an
established form in your language's Go community — and sometimes the community just keeps the
Japanese term. **Use whatever real players in your language actually say**, not a literal
translation.

If in doubt, take a look at how the [OGS website](https://online-go.com) or your national Go
federation words things.

### Keep it short

Android screens are narrow, and buttons don't stretch. If a translation is much longer than the
English original it may get cut off or wrap awkwardly. Where you can, prefer the shorter phrasing.

### Tone

The app talks to the player in a friendly, casual way — "OK, let's try again. Your turn!" rather
than "The operation may now be repeated." Match that tone in your language, and use whatever level
of formality feels natural for a game app (informal "you" is usually right).

## Asking questions

If a string is ambiguous — and some of them are, out of context — use the **Comments** tab in the
Crowdin editor for that string. Questions there are welcome and are usually the fastest way to
resolve something. You can also
[open an issue on GitHub](https://github.com/acristescu/OnlineGo/issues/new).

## Adding a new language

If your language isn't in the list yet, just
[open a GitHub issue](https://github.com/acristescu/OnlineGo/issues/new) asking for it (mention the
language and, ideally, that you intend to translate it). It only takes a moment to enable.

## When will I see my translation in the app?

Translations are merged into the code and ship with the next release, so there's usually some delay
between finishing a language and seeing it on your phone. A language typically needs to be fairly
complete before it's switched on in the app — anything untranslated falls back to English, which
looks patchy if there's a lot of it.

Once it's live, you can pick your language inside the app under **Settings → Language** (and on
Android 13 and newer, also from Android's own per-app language settings).

## Credit

Translators are contributors. If you've put meaningful work into a language and would like to be
credited in the README, say so in an issue or a Crowdin comment — happy to add you.

---

**Thank you.** Every language added makes the game reachable for more people, and that's the whole
point.
