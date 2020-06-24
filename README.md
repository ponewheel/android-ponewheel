# pOneWheel 
[![CircleCI](https://img.shields.io/circleci/project/github/ponewheel/android-ponewheel/master.svg)](https://circleci.com/gh/ponewheel/android-ponewheel) [![API](https://img.shields.io/badge/API-21%2B-green.svg?style=flat)](https://android-arsenal.com/api?level=21) [![MIT License](https://img.shields.io/github/license/ponewheel/android-ponewheel.svg)](https://opensource.org/licenses/mit-license.php)

This repository contains the source code for the pOneWheel app.

> What's pOneWheel?

Looking for extended and hidden stats for your fancy new OneWheel? And what about logging of these stats to figure out where exactly did things go wrong and cause you to eat asphalt? This app does log all these stats and also shows footpads that are activated, lifetime miles on your OneWheel, trip top speed, and more. Additionally this app includes power settings like setting the OneWheel LED front/back lights and brightness, triggering vibrations based on battery levels, and ability to change the ride mode at any time.

> What's the plan?

We plan to get a new release with all the new features, bug fixes, etc on the Google Play store soon. There have been a good amount of changes and as soon as we get a stable release workflow and sign off on it, it will be posted. From there on the plan is to do continuous releases.

> How can I help?

For questions, to report bugs, or suggestions and feature requests, head over to the [Github Ponewheel Issues] page. If you'd like to contribute, please fork this repository and contribute back using pull requests.

> Where did it go? 

Yeah, about that... https://github.com/ponewheel/android-ponewheel/issues/113

> How can I build/install on my mobile device? 

```
git clone https://github.com/ponewheel/android-ponewheel.git
cd android-ponewheel 
./gradlew build -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home
./gradlew installDebug -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home
```

> When will you support The Pint/4210 hardware/4142 firmware/etc?

Head over to https://github.com/ponewheel/android-ponewheel/issues and jump in! We can always use the help. 



Special thanks to,
- The [Contributors] that awesomely picked up this project and ran with it - @ebabel, @wmaciel, @twyatt
- My lady Linda Luu for giving it the usability and graphics it desperately needed
- @christorrella for pushing me to open source

<!-- ![ponewheel logo](artwork/logo.png?raw=true) -->
<!-- ![main screenshot](https://lh3.googleusercontent.com/9H6BH3lNRwYY50xUHNbHnpy68aAvUzxEuhE2Y-dcRB84hSeJx0EHScNe7v01bJTF8w=h310-rw) -->


[Github Ponewheel Issues]: https://github.com/ponewheel/android-ponewheel/issues
[Contributors]: https://github.com/ponewheel/android-ponewheel/graphs/contributors
[OneWheel]: https://onewheel.com/
[Google Play]: https://play.google.com/store/apps/details?id=net.kwatts.powtools
[Google Play Beta]: https://play.google.com/apps/testing/net.kwatts.powtools

