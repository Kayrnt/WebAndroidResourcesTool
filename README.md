WebAndroidResourcesTool
=======================

This project is design to provide some optimisations on your Android resources project.

It might be a bit hacky so if you want to clean it up, you can send pull requests.

Features
--------
* Based on the res/values/strings.xml, reorder, add non existing string reference and set empty value (will show warning)

Upcoming Features ( Roadmap )
-----------------------------
* add plurals supports for strings, better project structure management, some error detection & warning.
* optimizing values resources to suppress redondencies like common same values for a dimension or style defined for landscape or portrait instead of standard value.
* apply to images some automatic compression as ImageAlpha & ImageOptim (cf tinypng.com)

No ETA because I don't know how I much time I'll have to try to do this...
But if someone has ideas and want to add features, merge with another project... feel free to do so or contact me.

Using it
---------
* Zip your Android project
* Select it using "upload" button and press "start upload"
* Wait for the processing...
* Press "download file" to get your processed project repacked


Deploy your own
--------

* Download [the latest playframework version] (http://www.playframework.com/). The version 2.2.0 is used.
* Run this application with it ('play run' in the root directory) 
* Go to [http://localhost:9000/](http://localhost:9000/)

Libraries used
--------
* [Play Framework] (http://www.playframework.com/)
* [Akka] (http://akka.io/)
* [AngularJS] (http://angularjs.org/)
* [Bootstrap] (http://getbootstrap.com/)
* [JQuery] (http://jquery.com/)

Contact Me
--------

* [@Kayrnt](https://twitter.com/Kayrnt)
* [Porfolio](http://www.kayrnt.fr)

Licence
-------

This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
