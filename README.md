# Bamboo Satis Build

Bamboo Satis Build is a deployment task for Atlassian Bamboo that works together with [Satis Control Panel (SCP)](https://github.com/realshadow/satis-control-panel).

## How it works

Deployment task requires `composer.json` exported as shared build artifact to work correctly. Task will trigger partial 
update of Satis repository, thus rebuilding only the repository that this deployment task is used for. Partial update runs
*synchronously* through [Satis Control Panel (SCP)](https://github.com/realshadow/satis-control-panel) and will wait for the
rebuild to finish and thus complete or fail the deployment depending on the response from API. 

## Global configuration

Following options can be set in global configuration:

* SCP API URL address that should point to API endpoint, e.g. *http://example.com/control-panel/api/repository*
* VCS URL address should point to HTTP address of your repository in case you do not want to use SSH address, if configured, e.g. *http://example.com/scm/*
