pipeline {
  agent any
  parameters {
    booleanParam(name: 'PERFORM_RELEASE', defaultValue: false, description: 'Set to TRUE to deploy a release artifact and tag the source')
  }
  //tools {
      // Install the Maven version configured as "M3" and add it to the path.
      //maven "M3"
  //}
  stages {
    stage('Build') {
      steps {
        checkout([$class: 'GitSCM', branches: [[name: 'main']], extensions: [],
        	userRemoteConfigs: [[url: 'https://github.com/marioja/jenkins-test-plugin.git']]])
        // https://github.com/jenkinsci/lockable-resources-plugin.git
        //bat 'set'
        // To run Maven on a Windows agent, use
        bat "mvn -B -DskipTests -Dmaven.test.failure.ignore=true clean package"
      }
      post {
          // If Maven was able to run the tests, even if some of the test
          // failed, record the test results and archive the jar file.
        success {
            //junit '**/target/surefire-reports/TEST-*.xml'
            archiveArtifacts 'target/*.jar'
        }
      }
    }
    stage('OWASP Dependency-Track') {
      environment {
        RELEASE_VERSION_PARAMETERS = "${ params.PERFORM_RELEASE ? '-Dsha1='+ env.SVN_REVISION +' -Dchangelist=' : ' '}"
        BRANCH_NAME="main"
        MAVEN_PARAMS="-ntp -B"
        //JN = "${env.JOB_NAME.substring(0, env.JOB_NAME.lastIndexOf('/'))}"
      }
      steps {
        script {
          bat "echo RELEASE_VERSION_PARAMETERS=%RELEASE_VERSION_PARAMETERS% BRANCH_NAME=%BRANCH_NAME%"
          bat "mvn -DschemaVersion=1.1 ${env.RELEASE_VERSION_PARAMETERS} org.cyclonedx:cyclonedx-maven-plugin:2.1.0:makeAggregateBom ${env.MAVEN_PARAMS} "
          dependencyTrackPublisher artifact: 'target/bom.xml', projectName: env.JOB_NAME, synchronous: true, projectVersion: env.BRANCH_NAME
        }
      }
    }
  }
}
