Allows you to use any job as a template for another job with overridable parameters.

Creating a Template
===================
You create a template by creating any project type. Just check the property "Allow this job to be used as a template".

Using a Template
================
* Create a new job of the same job type as the template
* Check the property "Use another job as a template" and enter the template's name
    * Ensure "Allow this job to be used as a template" is _not_ checked
* Click Apply. 
 
Whenever the template job is saved, its config will automatically sync down to its implementations. It will also sync if you edit the implementation (i.e. you cannot override the template)

Customizing the implementation
==============================
Every time a template or implementation is saved, they sync.
The implementation basically overwrites its config with that of the template.
There are a few fields, however, that do not fully get synced. One of those is the [Parameters](https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Build).

Whenever a template syncs:

* New parameters added to the template are added to the implementation
* Old parameters removed from the template are removed from the implementation    
    * Note that renaming a variable counts as a removal and addition - it's is not synched as a rename and you will lose any customization.
* Default Values are _not_ synced.

This is useful if you want to have one job for every branch. ```BRANCH``` can be a Parameter. On the template job the Default Value could be ```master``` and the implementation has ```release```.