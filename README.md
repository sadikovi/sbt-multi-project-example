# sbt-multi-project-example
sbt multi-project setup example


## Running Docker

```shell
# go to the project directory
cd ~/developer/your/project

# build docker image (let's call it spark:1.4.0)
docker build -tq spark:1.4.0 .

# once it is done, run container with image
docker run -it -p 30022:22 -p 34040:4040 -p 38080:8080 -p 38081:8081 --name=spark-container spark:1.4.0
```
