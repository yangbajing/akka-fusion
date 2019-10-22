import Commons._
import Dependencies._
import Environment._

buildEnv in ThisBuild := {
  sys.props
    .get("build.env")
    .orElse(sys.env.get("BUILD_ENV"))
    .flatMap {
      case "prod"  => Some(BuildEnv.Production)
      case "stage" => Some(BuildEnv.Stage)
      case "test"  => Some(BuildEnv.Test)
      case "dev"   => Some(BuildEnv.Developement)
      case _       => None
    }
    .getOrElse(BuildEnv.Developement)
}

scalaVersion in ThisBuild := versionScala213

crossScalaVersions in ThisBuild := Seq(versionScala212, versionScala213)

scalafmtOnCompile in ThisBuild := true

sonarUseExternalConfig in ThisBuild := true

lazy val root = Project(id = "akka-fusion", base = file("."))
  .aggregate(
    fusionInjects,
    fusionSchedulerServer,
    fusionJob,
    fusionLog,
    fusionDiscoveryServer,
    fusionDiscoveryClient,
    fusionHttpGateway,
    fusionActuator,
    fusionHttp,
    fusionHttpClient,
    fusionOauth,
    fusionNeo4j,
    fusionKafka,
    fusionMongodb,
    fusionCassandra,
    fusionElasticsearch,
    fusionSlick,
    fusionMybatis,
    fusionJdbc,
    fusionMail,
    fusionJson,
    fusionSecurity,
    fusionTest,
    fusionCore,
    fusionCommon,
    helloscalaCommon)
  .settings(Publishing.noPublish: _*)
  .settings(Environment.settings: _*)
  .settings(aggregate in sonarScan := false)
//.settings(
//  addCommandAlias("fix", "all compile:scalafix test:scalafix"),
//  addCommandAlias("fixCheck", "; compile:scalafix --check ; test:scalafix --check"))

lazy val fusionDocs = _project("fusion-docs")
  .enablePlugins(ParadoxMaterialThemePlugin)
  .dependsOn(
    fusionInjects,
    fusionSchedulerServer,
    fusionJob,
    fusionLog,
    fusionDiscoveryServer,
    fusionDiscoveryClient,
    fusionHttpGateway,
    fusionActuator,
    fusionHttp,
    fusionHttpClient,
    fusionOauth,
    fusionNeo4j,
    fusionKafka,
    fusionMongodb,
    fusionCassandra,
    fusionElasticsearch,
    fusionSlick,
    fusionMybatis,
    fusionJdbc,
    fusionMail,
    fusionSecurity,
    fusionTest,
    fusionCore,
    fusionCommon,
    helloscalaCommon)
  .settings(Publishing.noPublish: _*)
  .settings(
    Compile / paradoxMaterialTheme ~= {
      _.withLanguage(java.util.Locale.SIMPLIFIED_CHINESE)
        .withColor("indigo", "red")
        .withRepository(uri("https://github.com/akka-fusion/akka-fusion"))
        .withSocial(
          uri("http://akka-fusion.github.io/akka-fusion/"),
          uri("https://github.com/akka-fusion"),
          uri("https://weibo.com/yangbajing"))
    },
    paradoxProperties ++= Map(
        "github.base_url" -> s"https://github.com/akka-fusion/akka-fusion/tree/${version.value}",
        "version" -> version.value,
        "scala.version" -> scalaVersion.value,
        "scala.binary_version" -> scalaBinaryVersion.value,
        "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/$versionAkka",
        "akka.version" -> versionAkka))

lazy val fusionInjects = _project("fusion-injects")
  .dependsOn(fusionHttp, fusionDiscoveryClient, fusionTest % "test->test")
  .settings(libraryDependencies ++= Seq(_guice))

lazy val fusionHttpGateway = _project("fusion-http-gateway")
  .dependsOn(fusionHttp, fusionDiscoveryClient, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq())

lazy val fusionDiscoveryServer = _project("fusion-discovery-server")
  .dependsOn(fusionDiscoveryClient, fusionHttp, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= _akkaClusters)
  .settings(Publishing.noPublish)

lazy val fusionDiscoveryClient = _project("fusion-discovery-client")
  .dependsOn(fusionHttpClient, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_akkaDiscovery, _nacosClient) ++ _akkaHttps)

lazy val fusionSchedulerServer = _project("fusion-scheduler-server")
  .dependsOn(fusionJsonCirce, fusionHttp, fusionDiscoveryClient, fusionJob, fusionTest % "test->test")
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
        _postgresql) ++ _akkaClusters)

lazy val fusionJob = _project("fusion-job")
  .dependsOn(fusionJdbc, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_quartz))

lazy val fusionActuatorCluster = _project("fusion-actuator-cluster")
  .dependsOn(fusionActuator, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_akkaManagementClusterHttp))

lazy val fusionActuator = _project("fusion-actuator")
  .dependsOn(fusionJson, fusionDiscoveryClient, fusionTest % "test->test", fusionCore)
  .settings(
    libraryDependencies ++= Seq(
        _akkaManagement,
        _kamonStatusPage,
        _kamonAkka,
        _kamonAkkaHttp,
        _kamonSystemMetrics,
        _kamonLogback,
        _akkaHttpTestkit % Test))

lazy val fusionOauth = _project("fusion-oauth")
  .dependsOn(fusionHttpClient, fusionSecurity, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_jwt, _jwtJson4s))

lazy val fusionMongodb = _project("fusion-mongodb")
  .dependsOn(fusionHttpClient, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= _mongodbs)

lazy val fusionHttp = _project("fusion-http")
  .dependsOn(fusionBoot, fusionHttpClient, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_akkaManagement))

lazy val fusionBoot = _project("fusion-boot").dependsOn(fusionTest % "test->test", fusionCore)

lazy val fusionHttpClient = _project("fusion-http-client")
  .dependsOn(fusionJson, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf,test,provided") ++ _akkaHttps)

lazy val fusionJsonCirce = _project("fusion-json-circe")
  .dependsOn(fusionTest % "test->test", helloscalaCommon)
  .settings(libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf,test,provided",
      _akkaHttp,
      _circeGeneric,
      _scalapbCirce))

lazy val fusionJson = _project("fusion-json")
  .dependsOn(fusionTest % "test->test", helloscalaCommon)
  .settings(libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf,test,provided",
      _akkaSerializationJackson,
      _akkaHttp,
      _json4s))

lazy val fusionLog = _project("fusion-log")
  .dependsOn(fusionTest % "test->test", fusionCommon)
  .settings(libraryDependencies ++= Seq(_logstashLogbackEncoder))

lazy val fusionCassandra = _project("fusion-cassandra")
  .dependsOn(fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= _cassandras)

lazy val fusionElasticsearch = _project("fusion-elasticsearch")
  .dependsOn(fusionJson, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= _elastic4ses)

lazy val fusionKafka = _project("fusion-kafka")
  .dependsOn(fusionJson, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq() ++ _akkaStreamKafkas)

lazy val fusionNeo4j = _project("fusion-neo4j")
  .dependsOn(fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_neotypes))

lazy val fusionSlick = _project("fusion-slick")
  .dependsOn(fusionJson, fusionJdbc, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_slickPg % Provided) ++ _slicks)

lazy val fusionMybatis = _project("fusion-mybatis")
  .dependsOn(fusionJdbc, fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_mybatisPlus, _lombok % Provided, _postgresql % Test, _mysql % Test))

lazy val fusionJdbc = _project("fusion-jdbc")
  .dependsOn(fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_hikariCP, _postgresql % Test, _mysql % Test))

lazy val fusionDoc =
  _project("fusion-doc").dependsOn(fusionTest % "test->test", fusionCore).settings(libraryDependencies ++= _pois)

lazy val fusionMail = _project("fusion-mail")
  .dependsOn(fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_jakartaMail))

lazy val fusionSecurity = _project("fusion-security")
  .dependsOn(fusionTest % "test->test", fusionCore)
  .settings(libraryDependencies ++= Seq(_bcprovJdk15on))

lazy val fusionTest = _project("fusion-test")
  .dependsOn(fusionCore, fusionCommon)
  .settings(Publishing.publishing: _*)
  .settings(
    libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        _akkaTypedTestkit,
        _akkaStreamTestkit,
        _scalatest))

lazy val fusionCore = _project("fusion-core")
  .dependsOn(fusionProtobufV3, fusionCommon)
  .settings(Publishing.publishing: _*)
  .settings(
    libraryDependencies ++= Seq(_requests, _akkaTypedTestkit % Test, _akkaStreamTestkit % Test, _scalatest % Test))

lazy val fusionProtobufV3 = _project("fusion-protobuf-v3")
  .enablePlugins(AkkaGrpcPlugin)
  .dependsOn(fusionCommon)
  .settings(Publishing.publishing: _*)
  .settings(
    PB.protocVersion := "-v371",
    libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf,provided",
        _akkaGrpcRuntime,
        _akkaDiscovery))

lazy val fusionCommon = _project("fusion-common")
  .dependsOn(helloscalaCommon)
  .enablePlugins(BuildInfoPlugin)
  .settings(Publishing.publishing: _*)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
        startYear,
        organization,
        organizationName,
        organizationHomepage,
        scalacOptions,
        javacOptions,
        version,
        scalaVersion,
        sbtVersion,
        sbtBinaryVersion,
        git.gitCurrentTags,
        git.gitDescribedVersion,
        git.gitCurrentBranch,
        git.gitHeadCommit,
        git.gitHeadCommitDate),
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoPackage := "fusion.version",
    buildInfoObject := "Version",
    libraryDependencies ++= Seq(_scalatest % Test))

lazy val helloscalaCommon = _project("helloscala-common")
  .settings(Publishing.publishing: _*)
  .settings(libraryDependencies ++= Seq(
      _jacksonAnnotations,
      _uuidGenerator,
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      _scalaCollectionCompat,
      _scalaJava8Compat,
      _akkaTypedTestkit % Test,
      _akkaStreamTestkit % Test,
      _scalatest % Test) ++ _akkas ++ _logs)

def _project(name: String, _base: String = null) =
  Project(id = name, base = file(if (_base eq null) name else _base))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(basicSettings: _*)
    .settings(Publishing.publishing: _*)
//.settings(addCompilerPlugin(scalafixSemanticdb))
