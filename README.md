```
docker run --name mysql \
    -e MYSQL_DATABASE=testdb -e MYSQL_ROOT_PASSWORD=my-secret-pw \
    -d -p 13306:3306 mysql:latest
```

```
docker run --rm --name mongo -d -p 27017:27017 mongo:4
```