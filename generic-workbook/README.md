# Generic Workbook

This submodule contains a generic version of the intens-db-service workbook.

The three ways to use this workbook is in kubernetes via helm, in docker/podman, or directly as a jar file.

The project contains liquibase migrations to start out a database with.
Those are meant as a base layer to which you apply project-specific changes.

It is also possible to provide your own base layer for each of the three methods.

## Jar usage

The `workbook.jar` file you get from `./mvnw package` is the fastest way to get started locally.

On startup it looks for the folder `./liquibase/changes` from the folder it gets started in.
It will then execute all liquibase migrations in that folder.

To provide your own base layer for liquibase you have to set the `spring.liquibase.change-log` property to point to your file.<br>
Example:
```sh
java -Dspring.liquibase.change-log=file:liquibase/db.changelog-master.xml -jar workbook.jar
```
Alternatively for ease of use, an application.properties file can also be used to set the property.

## Docker usage

The docker-compose.yaml file provided in the intens app starting template already is set up to use the
liquibase migrations from their default path:
```yaml
  workbook:
    image: ghcr.io/semaforinformatik/workbook:3.4
    ...
    volumes:
      ...
      - ../db/liquibase/changes:/workspace/liquibase/changes
```
You can change the directory that gets mounted to the `/workspace/liquibase/changes` folder to any folder containing your migrations.

To use your own base layer you will need to change the folder that gets mounted to not only mount the changes,
but the whole liquibase directory with the master file.

You also need to set the `spring.liquibase.change-log` environment variable in the container.
Example:

```yaml
  workbook:
    image: ghcr.io/semaforinformatik/workbook:3.4
    environment:
      - spring.liquibase.change-log=file:liquibase/db.changelog-master.xml
    volumes:
      ...
      - ../db/liquibase:/workspace/liquibase
```

## Helm chart

The helm chart here is a library chart, this is to allow the chart to import the liquibase migrations in the project you use it in.

To use the chart you need to add it as a dependency to your own helm chart and just place a single yaml file in the templates folder containing:
```gotmpl
{{ include "workbook.main.tpl" . }}
```
After that you just need to modify the values.yaml file with the provided variables, and make a folder called `liquibase` inside your chart's
root.

Inside the liquibase folder's `changes` directory will be your migrations.
When executing `helm install` on your chart using the library chart, the contents of the liquibase directory will be added to the
kubernetes deployment.

To use your own base layer, simply put the master liquibase file and the rest into the `liquibase` folder and set the value
`useCustomInit` inside your `values.yaml` file to true.

You can also pass custom environment values to for example set the access filter via the `customEnv` variable in your `values.yaml`.

To use your own statically provided keys for jwt auth, you also make a new folder in the chart root called `keys`, with the keys inside under the names
`app.key` and `app.pub`.
