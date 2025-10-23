# Workbook

This repository contains the previously separate `intens-db-service`, `gendas` and `generic-workbook` merged into one project
for ease of use, as these were not used separately anyway.

The generic-workbook submodule is still kept separate as the db-service still gets used in other projects like the ldpem workbook.
The goal is to one day also have that be the generic workbook.


## Package

By default, both db-service and generic-workbook are built when executing a maven command.
You can package the submodules individually by using the `--projects` CLI argument.

`./mvnw clean package --projects db-service`


For the commands that are only relevant to one module, the poms are configured in a way to only run them
in their respective module.
That mainly concerns the `deploy` and `spring-boot:build-image` commands.

So to make an OCI image of the workbook with the newest db-service you can just run this command right in the root directory:

`./mvnw clean spring-boot:build-image`
