# sandriosCamera 

[![sandrios studios](https://img.shields.io/badge/sandrios-studios-orange.svg?style=flat)](http://sandrios.com)  [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-sandriosCamera-yellow.svg?style=flat)](https://android-arsenal.com/details/1/4962#) [![Build Status](https://travis-ci.org/sandrios/sandriosCamera.svg?branch=master)](https://travis-ci.org/sandrios/sandriosCamera) ![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat) [ ![Download](https://api.bintray.com/packages/sandriosstudios/android/sandriosCamera/images/download.svg) ](https://bintray.com/sandriosstudios/android/sandriosCamera/_latestVersion)


Camera Library for Android

![8]

sandrios camera allows developers to integrate image and video capturing without dealing with the complicated camera APIs.

It includes image picker interface inside the cameraview enabling the user to access recent media from inside the camera module.

<img src="https://github.com/sandrios/sandriosCamera/blob/master/static/with_picker.png" width="300px" />
<img src="https://github.com/sandrios/sandriosCamera/blob/master/static/without_picker.png" width="300px" />

Download
--------
You can download an aar from GitHub's [releases page][1].

Or use Gradle:

```gradle
repositories {
   jcenter()
}

dependencies {
  compile 'com.sandrios.android:sandriosCamera:1.0.8'
}
```

Or Maven:

```xml
<dependency>
  <groupId>com.sandrios.android</groupId>
  <artifactId>sandriosCamera</artifactId>
  <version>1.0.8</version>
  <type>pom</type>
</dependency>
```


ProGuard
--------
Depending on your ProGuard (DexGuard) config and usage, you may need to include the following lines in your proguard.cfg

```pro

-keep public class com.sandrios.** { *; }

-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

-dontwarn android.support.**

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }

-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}
```

How do I use Sandrios Camera?
-------------------

Please check the sample project included for more examples:

```java
  private static final int CAPTURE_MEDIA = 368;

  // showImagePicker is boolean value: Default is true
  private void launchCamera() {
      new SandriosCamera(activity, CAPTURE_MEDIA)
                                  .setShowPicker(showImagePicker)
                                  .setVideoFileSize(15) //File Size in MB: Default is no limit
                                  .setMediaAction(CameraConfiguration.MEDIA_ACTION_VIDEO) // default is CameraConfiguration.MEDIA_ACTION_BOTH
                                  .launchCamera();
  }

  @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);

         if (requestCode == CAPTURE_MEDIA && resultCode == RESULT_OK) {
             Log.e("File", "" + data.getStringExtra(SandriosCameraConfiguration.Arguments.FILE_PATH));
             Toast.makeText(this, "Media captured.", Toast.LENGTH_SHORT).show();
         }
     }
```

Status
------

- Video file size limit
- Image crop added
- Flash Mode (Testing Needed)

Comments/bugs/questions/pull requests are always welcome!

Compatibility
-------------

 * **Android SDK**: Sandrios Camera requires a minimum API level of 14.

Download
-------

You may also find precompiled aar on the [releases page][1].

Getting Help
------------
To report a specific problem or feature request, [open a new issue on Github][4]. For questions, suggestions, or
anything else, email mailto:github@sandrios.com

Contributing
------------
Before submitting pull requests, contributors must sign Google's [individual contributor license agreement][5].

Thanks
------
* [**Glide**][6] for the [Image Loading Framework]
* [**Ted Permission**][7] for the Permission Management in Android 23+
* [**uCrop**][10] for the Image Cropping
* Everyone who has contributed code and reported issues!

Author
------
[sandrios studios][3] - @sandrios on GitHub

License
-------
MIT. See the [LICENSE][9] file for details.

Disclaimer
---------
This is not an official Google product.

[3]: https://www.sandrios.com
[1]: https://github.com/sandrios/sandriosCamera/releases
[2]: https://github.com/sandrios/sandriosCamera/wiki
[4]: https://github.com/sandrios/sandriosCamera/issues
[5]: https://developers.google.com/open-source/cla/individual
[6]: https://github.com/bumptech/glide
[7]: https://github.com/ParkSangGwon/TedPermission
[8]: https://github.com/sandrios/sandriosCamera/blob/master/static/sandrios_studios.png
[9]: https://github.com/sandrios/sandriosCamera/blob/master/LICENSE
[10]: https://github.com/Yalantis/uCrop
