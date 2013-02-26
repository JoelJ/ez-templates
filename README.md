Allows you to use any job as a template for another job.

Creating a Template
===================
You create a template by creating any project type. You check the property "Allow this job to be used as a template".
Now any job can use this job as a template.

Using a Template
================
* Create a new job by either selecting the same job type as the template or by selecting "Copy existing Job" and give it the template's name.
* Change the properties for that job so that "Allow this job to be used as a template" is *not checked* and "Use another job as a template" *is checked*.
* Click save. Whenever that job is saved or the template job is saved this job will automatically sync with it.

Customizing Template Implementations
====================================
Every time a template or implementation is saved, they sync.
The implementation basically overwrites it's config with the config of the template.
There are a few fields, however, that do not fully get synced. One of those is the Parameters.

Whenever a template syncs, only new parameters added to the template are copied over, or old parameters that were removed from the template are removed on the implementation (this includes renaming a variable).
If you change the default value in either the template or the implementation, that value is not synced.

This is useful if you want to have one job for every branch. ```BRANCH``` can be a variable. On the template job the default value could be ```master``` and the implementation has ```release```.