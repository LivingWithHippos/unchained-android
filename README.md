<p align="center">
  <img width="300" src="https://raw.githubusercontent.com/LivingWithHippos/unchained-android/master/extra_assets/graphics/logo.svg">
</p>

# Unchained for Android

[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)   [![API](https://img.shields.io/badge/API-22%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=22)    [![Build Status](https://img.shields.io/github/workflow/status/LivingWithHippos/unchained-android/Build)](https://github.com/LivingWithHippos/unchained-android/actions)    [![Play Store](https://img.shields.io/badge/play%20store-available-brightgreen)](https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained)      [![F Droid](https://img.shields.io/f-droid/v/com.github.livingwithhippos.unchained)](https://f-droid.org/packages/com.github.livingwithhippos.unchained/) [![translated](https://localization.professiona.li/widgets/unchained-for-android/-/strings/svg-badge.svg)](https://localization.professiona.li/engage/unchained-for-android/)


<a href='https://f-droid.org/packages/com.github.livingwithhippos.unchained/'><img  alt='Get it on F Droid' src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="75"/></a>  <a href='https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height="75"/></a> 





App to interact with [Real Debrid](https://real-debrid.com/) APIs.

### What is Real Debrid :question:

Real Debrid is a service to download files from hosting websites and the torrent network.
Files are downloaded directly on their servers that you can then use for your downloads.
They provide high speeds for a lot of services like Mega and RapidGator without needing 
all their premium accounts, and can also stream media files directly. 
**N.B. Real Debrid is a (cheap) paid service**

### Features :memo:

You can take a look at the project [here](https://github.com/LivingWithHippos/unchained-android/projects/1) for general status.

- [x] magnets/torrents support
- [x] file hosting services support
- [x] streaming support (needs a player that supports streaming like mpv or VLC)
- [x] search websites for files with plugins
- [x] containers support
- [x] user info
- [x] themes

### Screenshots :iphone:

| User  | Downloads List | Download Details | New Download | Search |
| ------------- | ------------- | ------------- |------------- |------------- |
| <img width="150" src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png?raw=true" alt="User Screen"> | <img width="150" src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png?raw=true" alt="List of downloads"> | <img width="150" src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png?raw=true" alt="Download details screen">  | <img width="150" src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png?raw=true" alt="New download screen">  | <img width="150" src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png?raw=true" alt="Search screen">  |


### Installation :calling:

You have multiple options to install Unchained for Android:

1. Get the [latest published release](https://github.com/LivingWithHippos/unchained-android/releases) from GitHub
2. Get the latest build's zipped APK (possibly unstable) from [this link](https://nightly.link/LivingWithHippos/unchained-android/workflows/build.yaml/master) (master branch only) or from one of the [Actions](https://github.com/LivingWithHippos/unchained-android/actions) (you need to be logged in GitHub): click on the latest working workflow and scroll to the bottom of the summary section, extract the APK and install it
3. Get the app on [Play Store](https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained)
4. Get the app on [F-Droid](https://f-droid.org/packages/com.github.livingwithhippos.unchained/)

### Developing and Contributing :writing_hand:

## [![Repography logo](https://images.repography.com/logo.svg)](https://repography.com) /
[![Issue status graph](https://images.repography.com/28505435/LivingWithHippos/unchained-android/recent-activity/9be46c12746e55ef26535ea523c2bda5_issues.svg)](https://github.com/LivingWithHippos/unchained-android/issues)


Contributions are welcome. You can use the [discussion tab](https://github.com/LivingWithHippos/unchained-android/discussions) to ask for help setting up the project. At the moment at least Android Studio 2021.1.1 is needed to build the project.

The dev branch is the one where the development happens, it gets merged into master when a release is ready.

A debug version is available, it reports automatically any crash information, it can be useful to help me debug errors.

This app is written in Kotlin and uses the following architectures/patterns/libraries:

MVVM architectural pattern, Dagger-Hilt for dependency injection, Data Binding for managing ui-data relations, Navigation, Moshi, Retrofit, OkHTTP, Room, Coroutines, Flow, Livedata, Coil

The app is available in English, Italian and French, you can contribute to those or add a new language [here](https://localization.professiona.li/engage/unchained-for-android/) (much appreciated!)

There's a work in progress [wiki page](https://github.com/LivingWithHippos/unchained-android/wiki/Search-Engine) for creating search plugins.

### Donate :coffee:

You can use [my referral link](http://real-debrid.com/?id=78841) to get Real Debrid premium.

Offer me coffee or a beer with Liberapay (set renewal to manual to avoid recurring donation) <noscript><a href="https://liberapay.com/LivingWithHippos/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a></noscript>

Send a tip with [Brave.](https://brave.com/liv466)

Send me a Bitcoin? Aha ha, just kidding… unless..?

<details>
<summary>Algorand (ALGO)</summary>
<br>
TO5D7VGONQRZR7P52EF2C3RJWLYNDA3E53F6SO3XCEGUHMSS3EH3D3TG6I
</details>

<details>
<summary>Bitcoin (BTC)</summary>
<br>
1PNZXRz77idWGhbMTRTG8iAuqnYY6tatb7
</details>

<details>
<summary>Ethereum (ETH)</summary>
<br>
0xf97bb71c898ac6d71c9fe065138b7134009f0599
</details>

<details>
<summary>Litecoin (LTC)</summary>
<br>
LWeoBVVmaYAiZ3oGaLAV9sV2dvY62XxdCF
</details>

### Credits :crown:

#### Beta testers

- Oathzed

#### Donors

- DaisyF8
- Roadhouse

#### Translators

- edgarpatronperez (spanish)

#### Media

Logo and symbols inspired by [minimal logo design set](https://www.rawpixel.com/image/843352/minimal-logo-designs-set) offered by [rawpixel.com](https://www.rawpixel.com)
Icons by [Fluent UI](https://www.svgrepo.com/collection/fluent-ui-icons-outlined/) offered by [SVG Repo](https://www.svgrepo.com/)
Backgrounds courtesy of [haikei](https://haikei.app/) and [SVG Backgrounds](https://www.svgbackgrounds.com/)

### Thanks, Mr. Unchained :muscle:

<a href="https://imgbb.com/"><img src="https://i.ibb.co/grzjQsT/Oliva.jpg" width=300 alt="Mr. Unchained" border="0"></a>

