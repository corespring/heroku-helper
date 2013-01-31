# heroku-helper

A utility to allow you to run scripts as part of a heroku push/rollback

## Running the interactive console
    java -jar heroku-helper-0.1-one-jar.jar

## Running a command
    java -jar heroku-helper-0.1-one-jar.jar push herokuapp branch

When the helper is launched it looks for 2 files to load from the same folder that you just launched it in:
* .heroku-helper.conf
* .heroku-helper-env.conf

## Available commands:
* help - display help
* repos - list all the heroku repos for this folder
* repo {name} - list information for the given repo
* push {gitRemoteName} {branch} - push this code base to the git remote using the given branch
* rollback {app} {versionId} - roll the heroku app to the given versionId (short commit hash)
* releases {app} - list the releases for the given app
* set-env-vars - set all the environment variables
* folder-info - info about the current directory (path + short commit hash)

## .heroku-helper.conf
This file defines scripts that should be run for a specific heroku app.

   # This script will run on launch - so you can validate that the environment is setup
   startupValidation: "blah.sh"
   # A list of configurations
   appConfigs:[

         {
            #heroku app name
            name:"corespring-app"
            #this is what its called by the local git repo
            gitRemoteName : "corespring-app"

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

To set the vars for an app call 'set-env-vars'

## System requirements
* To interact with the Heroku REST Api, the helper uses the ~/.netrc file for authentication (as does the heroku toolbelt).
* git
* heroku toolbelt


## Building
We use sbt 12.x

    sbt test one-jar



