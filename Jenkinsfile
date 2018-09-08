node {
    try {
        def mvnHome = tool 'M3'
        env.JAVA_HOME="${tool 'jdk-oracle-8'}"
        env.PATH="${env.JAVA_HOME}/bin:${mvnHome}/bin:${env.PATH}"

        env.NEXUS_ROOT="http://nexus.usethesource.io"

        stage('Clone'){
            checkout scm
        }

        stage("Reset") {
            sh "rm -rf site"
            sh "mkdir site"
        }

        stage("Download & Unpack") {
            // we use the stable link, as the update site script contains a fixed version identifier, the lastest nexus release is not the same as the stable release.
            // downside is that this Jenkinsfile has to be re-run after a new stable release, as it takes a minute for the update site to process it
            sh 'curl -L "https://update.rascal-mpl.org/console/rascal-shell-stable.jar" > unstable.jar'
            // for the unstable, we do use nexus, to avoid always being one commit behind the unstable release on the slower-to-update update site
            sh 'curl -L "http://nexus.usethesource.io/service/local/artifact/maven/content?g=org.rascalmpl&a=rascal&r=snapshots&v=LATEST" > stable.jar'

            sh 'jar xf stable.jar boot/courses'
            sh 'mv boot/courses site/stable'

            sh 'jar xf unstable.jar boot/courses'
            sh 'mv boot/courses site/unstable'

            sh 'rm -r *.jar boot/'
        }

        stage("Fixup") {
            sh "find site/stable -name *.html -print0   | xargs -0 sed -i 's,\\(src\\|href|action\\)=\"/,\\1=\"/stable/,g'"
            sh "find site/unstable -name *.html -print0 | xargs -0 sed -i 's,\\(src\\|href|action\\)=\"/,\\1=\"/unstable/,g'"
        }

        stage("Compress") {
            // compress the large HTML pages with some kind of minification?
        }

        withMaven(maven: 'M3', jdk: 'jdk-oracle-8', options: [artifactsPublisher(disabled: true), junitPublisher(disabled: true)] ) {
            stage("Merge indexes") {
                sh 'mkdir site/search'
                sh 'mvn clean compile'
                sh 'mvn exec:java -Dexec.args="site/search/stable site/stable/"'
                sh 'mvn exec:java -Dexec.args="site/search/unstable site/unstable/"'
            }

            stage('Package') {
                sh 'tar czf full-site.tar.gz site/'
            }
            
            stage('Deploy') {
                sh 'mvn deploy:deploy-file -DgeneratePom=false -Dpackaging=tar.gz -Dfile=full-site.tar.gz'
            }
        }
    } catch(e) {
        slackSend (color: '#d9534f', message: "FAILED: <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
        throw e
    }
}