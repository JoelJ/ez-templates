Allows you to use any job as a template for another job with overridable parameters.

Creating a Template
===================
Any Jenkins job can be used as a template
* Select "Allow this job to be used as a template"

A template can be a fully runnable job in its own right or left disabled and used only as a parent for "real" jobs.

Using a Template
================
Any Jenkins job can inherit from a template of the same job type 
* Select "Use another job as a template" and enter the template's name
    * Ensure "Allow this job to be used as a template" is _not_ checked
* Click Apply. 

How it works
============
The implementation overwrites its config with that of the template _except_ the [Parameters][1],
which retain their default values, and items covered by specific flags.

Synchronisation happens whenever an implementation job or its template is saved.

Customizing the implementation
------------------------------
There are a few fields, however, that do not fully get synced. One of those is the [Parameters][1] section.

Whenever a template syncs:

* New parameters added to the template are added to the implementation
* Old parameters not in the template are removed from the implementation    
    * :exclamation: Renaming a template parameter counts as a removal and addition - it is not synched as a "rename" and you will lose any customisation.
* Existing parameters are synchronised
    * Default Values are _not_ synced

Usecases
========
Not everything can be parameterised in a Jenkins job config. Here's a few sample uses 

* SCM Repository URL can be a param, allowing completely different projects to be built with the same template
    * Alternatively the param could simply the Git branch (e.g. ```master```, ```feature```)
* Combined with [NodeLabel plugin][2], implementations can specify build node requirements
    * e.g. Only build on Linux or boxes with IPv6
* [Parameterized Trigger][3] and [Conditional BuildStep][4] plugins are the basis of many flexible configurations based on job parameters        

[1]: https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Build
[2]: https://wiki.jenkins-ci.org/display/JENKINS/NodeLabel+Parameter+Plugin
[3]: https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Trigger+Plugin
[4]: https://wiki.jenkins-ci.org/display/JENKINS/Conditional+BuildStep+Plugin