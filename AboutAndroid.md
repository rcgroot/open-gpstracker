As published on [JavaBlog](http://java.sogeti.nl/JavaBlog/tag/android/)

# Introduction #

Android is a new operating system and platform for mobile devices. Where did it come from and where is it going?

Well, back in 2005 Google bought the two year old startup Android Inc. Android operated in secret, with evidence pointing to that Android was about wireless and mobile software. Google just mentioned that it acquired talented engineers and great technology`[1]`. The two next big things were the formation of the Open Handset Alliance`[2]` and the SDK release`[3]` in 2007. The Open Handset Alliance wants to improve the mobile experience for consumers. Two of the founding members of the alliance, namely T-Mobile and HTC, are currently most visible by bringing Android to consumers with the G1 smart phone. Google has been a driving force behind the platform software. One thing was the $10 million dollar developer challenge to kickstart community developed software. The result was a plethora of applications on the beta version of the Android platform. The continuation of this initial boost can be seen in the Android application Market to this day.

The Android platform is open. Contributers and users are mobile operators, handset manufacturers, semiconductor companies, software companies and more. This openness is not only in words, it is also clearly demonstrated by the openness of the Android sources. The Android sources are available under the open source Apache License`[4]`. This enables Android to move beyond a single operator, a single brand, a single software vendor or even beyond a fixed device concept.

So with Android in place, Google and its partners are in a position to challenge and change  the mobile market. It is time to welcome our new Google overlords or start wearing tinfoil hats. At the very least it is good to take a closer look at what Android entails.

  1. [Google Buys Android for Its Mobile Arsenal](http://www.businessweek.com/technology/content/aug2005/tc20050817_0949_tc024.htm)
  1. [Industry Leaders Announce Open Platform for Mobile Devices](http://www.openhandsetalliance.com/press_110507.html)
  1. [Open Handset Alliance Releases Android SDK](http://www.openhandsetalliance.com/press_111207.html)
  1. [Android Open Source Project](http://source.android.com/)

# Overview #

What is Android? There are a couple of ways to answer that question. We can examine the composition, consider how to build with Android or for what purposes Android can fulfill.

_Android is a mash-up of many portable open source projects integrated and exposed by Java libraries to form a mobile device platform._

Taking a look at the architecture`[1]` of Android there are quite a few components. Many are not new and are respectable software projects in their own right. For example, the Linux`[2]` kernel is used to control the hardware. Linux has been ported to many architectures including successful ports to mobile devices. There are Java libraries from Apache Harmony`[3]`, SSL libraries from OpenSSL`[4]`, webpage rendering from Webkit`[5]`, a database from SQLite`[6]` and font rendering from FreeType`[7]`. Driving the integration of all those packages is the Dalvik virtual machine. It runs all the Java applications which combine all the resources into a user experience. Dalvik is based on the open specification of Java, but not a certified Java VM. So Dalvik leverages the Java experience of developers, but keeps the VM free of interference by their mobile competitor Sun.

_Android is there to build on and where a developer can brings his or her experience to bear to create applications._

"Vell, Android's just zis thing, you know?"`[8]` Knowing what Android is composed of is the beginning of getting to know it. First of all, Android is ready, usable and directly available to consumers worldwide. So anything you want to try, build or see can be done with the real thing. And secondly, the SDK`[9]` has been available since August 2008. What can be expected after installing the SDK? A developer can expect to feel at home. The workbench is a familiar one because the SDK provides you with editors, a debugger, a runtime and controls all gift wrapped as Eclipse plug-ins. An Android project does not differ much from a Java project. So bring you experience with Subversion, PMD, Findbugs, JUnit, Mylyn, code completion, inline docs, outlines, configuration, debugging, refactoring and all other sugar coating found plugged in Eclipse. Prepare to learn rich, slightly rough new API's, new life cycles, new program flows and working with new concepts. These are not earthmoving differences, but just enough to test agility and open mindedness.

_Android is leverage to open new markets and a new means to deliver services._

So there is this attractive platform and there is a large pool of experienced talent ready to be let lose. So what kind of software can we expect? Most notable of course is that Android is mobile. That means that new applications are feasible that add value by being used on location. A simple email client can outshine a full fledged client because runs is on a mobile device. A bar code reader`[10]` becomes a powerful consumer tool because it is mobile. Mobility is not just on the receiving side. New applications are possible when sending the location of the device as input. All kinds of variants of localized search opportunities open up.`[11,12]` This goes in general for all sensory input, the information from the sensors can be used to create more useful applications.

_Android is a means to make a compelling proposition to users._

The issue with mobile applications is that presentation is harder. User input is harder, screens are smaller, processing power is less and bandwidth is limited. The same solutions for application delivery as on the desktop are used on the mobile device. There are for instance websites with layouts specially tailored for display on mobile devices. These kind of applications are fast in creation, need little extra skills and rely on existing infrastructure. On the downside these application lack the sensor input, are slow in responsiveness and can not utilize the full power of a device. On the complete other side of the spectrum are custom rendered applications. Android has OpenGL which allows the application builder to bring an responsive, lively application with brings all visual power available to bear. This give the user a unique experience and can make an applications enjoyable. This is most often the case with games. These type of applications require a lot of effort and skills to build. In the middle of the spectrum is the component based user interface. Basic widgets such as the label, button and lists can be organized to present data and gather input. These require platform knowledge, are fast, can utilize the full power of the device and can fit nicely with the expected behavior of the device. The drawbacks are that they are platform specific, limited by the offerings of the platform and not at all exciting to use. Of these three options degrees of hybrids are possible. For example a components interface with a web page rendering, 3D widgets as components or component interface portals to web applications.

To conclude: Android is powerful, ready and able to reach people.

  1. [What is Android?](http://developer.android.com/guide/basics/what-is-android.html)
  1. [Linux kernel release 2.6](http://kernel.org/)
  1. [Apache Harmony](http://harmony.apache.org/)
  1. [OpenSSL: The Open Source toolkit for SSL/TLS](http://www.openssl.org/)
  1. [The WebKit Open Source Project](http://webkit.org/)
  1. [SQLite](http://www.sqlite.org/)
  1. [The FreeType Project](http://www.freetype.org/)
  1. [Misquoting of Gag Halfrunt](http://en.wikipedia.org/wiki/Zaphod_Beeblebro)
  1. [Android Software Development Kit (SDK)](http://developer.android.com/sdk/)
  1. [ShopSavvy Android](http://www.biggu.com/apps/shopsavvy-android/)
  1. [Enkin: Navigation reinvented](http://www.enkin.net/)
  1. [cab4me - Get a cab. Anywhere. Anytime. ](http://www.cab4me.com/)