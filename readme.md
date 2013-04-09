About
==============
This is a version of Apache Shindig 2.0 extended with Spaces for Moodle plugins that
bring OpenSocial gadgets to the Moodle.

Read more and see screenshots at [this blog](http://vohtaski.blogspot.com/2011/09/bring-opensocial-gadgets-to-moodle_22.html)

It works with [this Moodle plugin](https://github.com/vohtaski/shindig-moodle-mod).

Requirements
==============
* Moodle 2.1 (was not checked on previous versions!)
* [Moodle plugin](https://github.com/vohtaski/shindig-moodle-mod)

Installation
==============

## Install Moodle plugin

See instructions in [Moodle plugin](https://github.com/vohtaski/shindig-moodle-mod).

## Install Shindig for Moodle

If you want to support OpenSocial APIs, you should
connect your own Shindig installation to your Moodle database. This
shindig code has Moodle-extensions to match OpenSocial APIs with Moodle database schema.

### Get extended Shindig!  
    
    $git clone git://github.com/vohtaski/moodle-shindig.git
    
    
### Add ssl keys
   
    $mkdir ssl_keys
    $cd ssl_keys
    $openssl req -newkey rsa:1024 -days 365 -nodes -x509 -keyout testkey.pem -out testkey.pem -subj '/CN=mytestkey'
    $openssl pkcs8 -in testkey.pem -out oauthkey.pem -topk8 -nocrypt -outform PEM
    
   
### Copy the following three files 

You can save all your specific configurations in these three files. When you
update the code, they will not be touched. When you make production
version of your shindig, these files are taken into account.

    $cp java/common/conf/shindig.properties java/common/conf/shindig.properties_production
    $cp java/samples/src/main/resources/socialjpa.properties java/samples/src/main/resources/socialjpa.properties_production
    $cp java/server/src/main/webapp/WEB-INF/web.xml java/server/src/main/webapp/WEB-INF/web.xml_production

### Change the ssl keys information in shindig.properties_production
    
    shindig.signing.key-name=mytestkey
    shindig.signing.key-file=/path_to_shindig_branch/ssl_keys/oauthkey.pem
    

### Change your database information in socialjpa.properties_production
    
    db.driver=com.mysql.jdbc.Driver
    db.url=jdbc:mysql://localhost:3306/moodle
    db.user=shindig
    db.password=shindig
    db.write.min=1
    db.read.min=1
    jpa.socialapi.unitname=default
    shindig.canonical.json.db=sampledata/canonicaldb.json
    
    
### Change host and port settings for your Shindig in web.xml
    
    # You should specify which Shindig host will be run, e.g. , if you want
    # to run the Shindig host on your local machine, you should replace the
    # value "iamac71.epfl.ch" with "localhost" in
    # java/server/src/main/webapp/WEB-INF/web.xml line 58
    
    shindig.host=iamac71.epfl.ch
    aKey=/shindig/gadgets/proxy?container=default&amp;url=
    shindig.port=8080
    

### Change the url of your Moodle installation
    
    # In file: java/samples/src/main/java/org/apache/shindig/social/opensocial/jpa/SpaceDb.java
    # Change MOODLE_URL to your own url
    public static final String MOODLE_URL = "http://iamac71.epfl.ch/moodle";


### Change column name in person.db file. Only, if you do not use standard Moodle prefix for tables "mdl_"
    
    @Table(name = "mdl_user")
    

Compilation and running with make
=======================
Compile
-------

    $make
    
Run server at localhost
----------

    $make start # Shindig should be at localhost:8080

Clear all production temporal changes
---
  
    $make clean

Prepare .war files for Production
---

    $make production -> takes your config files "*_production" and builds production.war in the current directory based on them
    
!!! Compiled .war file should be renamed into ROOT.war on the Tomcat server.

Alternative starting with maven
===============================

    // To build the project
    $mvn -Dmaven.test.skip
    // To run the server on localhost (if not - put .war file into tomcat)
    $cd java/server
    $mvn jetty:run

License
=======

Apache Shindig with Spaces - ASF
------------------------------------------

    /**
     * Licensed to the Apache Software Foundation (ASF) under one
     * or more contributor license agreements. See the NOTICE file
     * distributed with this work for additional information
     * regarding copyright ownership. The ASF licenses this file
     * to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at
     * 
     *  http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing,
     * software distributed under the License is distributed on an
     * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     * KIND, either express or implied. See the License for the
     * specific language governing permissions and limitations
     * under the License.
     */
