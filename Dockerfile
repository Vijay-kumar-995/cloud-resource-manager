FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY Workload.java .
RUN javac Workload.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/Workload.class .
ENTRYPOINT ["java", "-Xms16m", "-Xmx128m", "Workload"]
