<p align="center">
  <img width="300" src="https://raw.githubusercontent.com/LivingWithHippos/unchained-android/master/extra_assets/graphics/logo.svg">
</p>

# Unchained for Android

[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)    [![Kotlin Version](https://img.shields.io/badge/kotlin-1.5.10-blue)](http://kotlinlang.org/) [![Android Studio](https://img.shields.io/badge/Android%20Studio-4.1%2B-brightgreen)](https://developer.android.com/studio)    [![API](https://img.shields.io/badge/API-22%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=22)    [![Build Status](https://img.shields.io/github/workflow/status/LivingWithHippos/unchained-android/Build)](https://github.com/LivingWithHippos/unchained-android/actions)    [![Play Store](https://img.shields.io/badge/play%20store-available-brightgreen)](https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained) [![translated](https://localization.professiona.li/widgets/unchained-for-android/-/strings/svg-badge.svg)](https://localization.professiona.li/engage/unchained-for-android/)



App to interact with [Real Debrid](https://real-debrid.com/) APIs.

### What is Real Debrid :question:

Real Debrid is a service to download files from hosting websites and the torrent network.
Files are downloaded directly on their servers that you can then use for your downloads.
They provide high speeds without premium accounts for a lot of services like Mega and RapidGator
and can also convert downloads of media files into streams. It offers a cheap premium service.

### Features :memo:

You can take a look at the project [here](https://github.com/LivingWithHippos/unchained-android/projects/1) for general status.

- [x] login with a temporary open-source token (it has some API limitations)
- [x] login with permanent [private API key](https://real-debrid.com/apitoken) (recommended)
- [x] user info
- [x] themes support
- [x] unrestrict links
- [x] containers support
- [x] magnets support
- [x] torrent support
- [x] streaming support (needs a player that supports streaming like VLC)
- [x] search magnet and torrents

### Screenshots :iphone:

| Home  | New Download | Download Details |
| ------------- | ------------- | ------------- |
| <img width="150" src="/extra_assets/screenshots/home.png?raw=true" alt="User Screen"> <img width="150" src="/extra_assets/screenshots/home_dark.png?raw=true" alt="Dark User Screen"> | <img width="150" src="/extra_assets/screenshots/new_download.png?raw=true" alt="New Download Screen">  | <img width="150" src="/extra_assets/screenshots/download_details_streaming.png?raw=true" alt="Download Details Screen">  |


| Torrent Download                                                                                             | Download List                                                                                   |
|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| <img width="150" src="/extra_assets/screenshots/torrent_details.png?raw=true" alt="Torrent Download Screen"> | <img width="150" src="/extra_assets/screenshots/download_list.png?raw=true" alt="Download List Screen"> |

### Installation :calling:

You have multiple options to install Unchained for Android:

1. Get the [latest published release](https://github.com/LivingWithHippos/unchained-android/releases) from GitHub
2. Get the latest build's zipped APK (possibly unstable) from [this link](https://nightly.link/LivingWithHippos/unchained-android/workflows/build.yaml/master) (master branch only) or from one of the [Actions](https://github.com/LivingWithHippos/unchained-android/actions) (you need to be logged in GitHub): click on the latest working workflow and scroll to the bottom of the summary section, extract the APK and install it
3. Get the [Play Store release](https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained)

### Developing and Contributing :writing_hand:

At the moment, the app is under heavy development under Android Studio beta. Contributions are welcome.

A debug version is available, it reports automatically any crash information, it can be useful to help me debug errors.

This app is written in Kotlin and uses the following architectures/patterns/libraries:

MVVM architectural pattern, Dagger-Hilt for dependency injection, Data Binding for managing ui-data relations, Navigation, Moshi, Retrofit, OkHTTP, Room, Coroutines, Flow, Livedata, Coil

The app is available in English, Italian and French, you can contribute to those or add a new language [here](https://localization.professiona.li/engage/unchained-for-android/) (much appreciated)

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

#### Media

Logo and symbols inspired by [minimal logo design set](https://www.rawpixel.com/image/843352/minimal-logo-designs-set) offered by [rawpixel.com](https://www.rawpixel.com)
Icons by [Fluent UI](https://www.svgrepo.com/collection/fluent-ui-icons-outlined/) offered by [SVG Repo](https://www.svgrepo.com/)

### Thanks, Mr. Unchained :muscle:

<a href="https://imgbb.com/"><img src="https://i.ibb.co/grzjQsT/Oliva.jpg" width=300 alt="Mr. Unchained" border="0"></a>

