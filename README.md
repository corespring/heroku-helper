# heroku-helper

A utility to allow you to run scripts as part of a heroku push/rollback

## Running the interactive console
    java -jar heroku-helper-0.1-one-jar.jar

## Running a command
    java -jar heroku-helper-0.1-one-jar.jar push herokuapp branch

When the helper is launched it looks for 2 files to load from the same folder that you just launched it in:
* .heroku-helper.conf
* .heroku-helper-env.conf - (You can also specify: HEROKU_HELPER_ENV_CONF env var to point to another env .conf file)

## Available commands:
* help - display help
* apps - list all the heroku applications configured for this folder
* info {heroku-app-name} - list information for the given heroku app
* push {heroku-app-name} {branch} - push the specified branch from this code base to the heroku app specified
* rollback {heroku-app-name} {versionId} - roll the heroku app to the given versionId (short commit hash)
* releases {heroku-app-name} - list the releases for the given heroku app name
* set-env-vars {heroku-app-name} - using the configuration information in .heroku-helper-env.conf apply the variables to the given heroku app (equivalent of calling `heroku config:set A="A"`)
* folder-info - information about the current directory (path + short commit hash)

## .heroku-helper.conf
This file defines scripts that should be run for a specific heroku app.

    # This script will run on launch - so you can validate that the environment is setup
    startupValidation: "blah.sh"
    # A list of configurations
    appConfigs:[

         {
            #heroku app name
            name:"corespring-app"
            
            push:{
               #optional - override the push command
               #default: git push ${gitRemote} ${branch}:master
               #tokens: gitRemote, branch
               cmd: "echo \"${gitRemote} ${branch}\""
               # an array of scripts to run
               # each of these scripts will be passed as the first argument
               # a path to a json file that contains the env vars for the given
               # heroku app.
               before:[ "deployment/common/backup_db.rb", ... ]
               after: [ "deployment/common/after_push.rb", ...]
            }
            rollback:{
               #options - override the rollback command
               cmd: "echo \"${version} ${app}\""
               #an array of scripts to run before rollback
               #each script will be passed 2 params:
               #1. the short commit has to rollback to
               #2. the environment variables for the targeted heroku release
               before:[ "deployment/common/backup_db.rb", ...]
               after: [ "deployment/common/after_rollback.rb", ...]
            }
         }
         ...
    ]

## .heroku-helper-env.conf
This file contains environment variables and other sensitive data for your heroku apps.

    environments:[
          {
             name:"my_heroku_app"
             vars : {
              VAR_ONE: "one"
              VAR_TWO: "two"
              ...
             }
          }
          ...
    ]

To set the vars for all apps call the 'set-env-vars'

## System requirements
* To interact with the Heroku REST Api, the helper uses the ~/.netrc file for authentication (as does the heroku toolbelt).
* git
* heroku toolbelt


## Building
We use sbt 12.x

    sbt test one-jar

### Release Notes

#### 0.2
- When pushing env vars are automatically set (like calling `set-env-vars` then `push`).
- When setting env vars - remove all the current vars before setting.
- Allow configuration of 'reserved env vars' in .heroku-helper.conf: `reservedEnvVars`. These vars won't be deleted
but can be overwritten.

#### 0.1
- Initial version



