<p align="center">
  <img width="300" src="https://raw.githubusercontent.com/LivingWithHippos/unchained-android/master/extra_assets/graphics/logo.svg">
</p>

# Unchained for Android

[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)    [![Kotlin Version](https://img.shields.io/badge/kotlin-1.4.10-blue)](http://kotlinlang.org/) [![Android Studio](https://img.shields.io/badge/Android%20Studio-4.1%2B-brightgreen)](https://developer.android.com/studio)    [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)    [![CircleCI](https://circleci.com/gh/LivingWithHippos/Keter-Escape/tree/master.svg?style=shield)](https://circleci.com/gh/LivingWithHippos/unchained-android/tree/master)    [![Play Store](https://img.shields.io/badge/play%20store-available-brightgreen)](https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained)



App to interact with [Real Debrid](https://real-debrid.com/) APIs.

### What is Real Debrid :question:

Real Debrid is a service to download files from hosting websites and the torrent network. Files are downloaded directly on their servers that you can then use for your downloads. 
They provide high speeds without premium accounts for a lot of services like Mega and RapidGator and can also convert downloads of media files into streams. It is a (cheap) paid service.

### Features :memo:

You can take a look at the project [here](https://github.com/LivingWithHippos/unchained-android/projects/1) for general status.

- [x] login with a temporary open-source token (it has some API limitation)
- [x] login with permanent [private API key](https://real-debrid.com/apitoken)
- [x] user info
- [x] dark mode
- [x] unrestrict links
- [x] magnets support
- [x] torrent support
- [x] streaming links (needs a player that supports streaming like vlc)

### Screenshots :iphone:

| Home  | New Download | Download Details |
| ------------- | ------------- | ------------- |
| <img width="150" src="/extra_assets/screenshots/home.png?raw=true" alt="User Screen"> <img width="150" src="/extra_assets/screenshots/home_dark.png?raw=true" alt="Dark User Screen"> | <img width="150" src="/extra_assets/screenshots/new_download.png?raw=true" alt="New Download Screen">  | <img width="150" src="/extra_assets/screenshots/download_details_streaming.png?raw=true" alt="Download Details Screen">  |


| Torrent Download                                                                                             | Download List                                                                                   |
|--------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| <img width="150" src="/extra_assets/screenshots/torrent_details.png?raw=true" alt="Torrent Download Screen"> | <img width="150" src="/extra_assets/screenshots/download_list.png?raw=true" alt="Download List Screen"> |

### Installation :calling:

You have multiple options to install Unchained for Android:

1. Get the [latest published release](https://github.com/LivingWithHippos/unchained-android/releases) from Github
2. Get the [latest build artifacts](https://app.circleci.com/pipelines/github/LivingWithHippos/unchained-android?branch=master) from CircleCI: expand the desired pipeline item -> select "build" -> select the "artifacts" tab
3. Get the [Play Store release](https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained)


### Developing and Contributing :writing_hand:

At the moment, the app is under heavy development under Android Studio stable. PRs are welcome.

This app is written in Kotlin and uses the following architectures/patterns/libraries:

MVVM architectural pattern, Dagger-Hilt for dependency injection, Data Binding for managing ui-data relations, Navigation, Moshi, Retrofit, OkHTTP, Room, Coroutines, Livedata, Glide

### Donate :coffee:

You can use [my referral link](http://real-debrid.com/?id=78841) to get Real Debrid premium. 

Offer me coffee or a beer with Liberapay (set renewal to manual to avoid recurring donation) <noscript><a href="https://liberapay.com/LivingWithHippos/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a></noscript> 

Send a tip with [Brave.](https://brave.com/liv466)

Send me a Bitcoin? Aha ha, just kidding.. unless..? (bitcoin:32XF4QYSnkHfQ8h37dTxrjd56vPn26v22f)

### Credits :crown:

Logo and symbols inspired by [minimal logo design set](https://www.rawpixel.com/image/843352/minimal-logo-designs-set)
 offered by [rawpixel.com](https://www.rawpixel.com)
 
### Thanks, Mr. Unchained :muscle:

<a href="https://imgbb.com/"><img src="https://i.ibb.co/grzjQsT/Oliva.jpg" width=300 alt="Mr. Unchained" border="0"></a>
