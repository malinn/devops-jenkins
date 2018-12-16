import groovy.json.JsonSlurper;
import javax.xml.bind.DatatypeConverter;

URL bitbucketUrl = "https://bitbucket.org/api/2.0/repositories/mediahealth".toURL();

def bitbucketProjectKey = "CRM";

// Open connection
URLConnection connection = bitbucketUrl.openConnection();

// TODO: how to use jenkins credentials here?
String username = ${bitbucketUser};
String password = ${bitbucketPassword};

// Create authorization header using Base64 encoding
String userpass = username + ":" + password;
String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(userpass.getBytes());

// Set authorization header
connection.setRequestProperty ("Authorization", basicAuth);

// Open input stream
InputStream inputStream = connection.getInputStream();

// Get JSON output
def response = new JsonSlurper().parseText(inputStream.text);
List repositories = response.values;

// Close the stream
inputStream.close();

def repositoriesList = [];

repositories.each { repository2 ->
  def projectKey = repository2.project.key
  if (projectKey != bitbucketProjectKey) {
    return
  }
  def projectName = repository2.project.name
  def repositoryName = repository2.name
  def repositorySshUrl
  repository2.links.each { key, val ->
    if (key != "clone") {
      return
    }
    val.each { cloneAddress ->
      if (cloneAddress.name == "ssh") {
        repositorySshUrl = cloneAddress.href

      }
    }
  }
  def repo = [ projectName: projectName, repositoryName: repositoryName, repositorySsh: repositorySshUrl ]
  repositoriesList.add(repo)
}

repositoriesList.each {repo ->
  //create main job
  println repo.projectName + " - " + repo.repositoryName
  println repo
  pipelineJob(repo.projectName + " - " + repo.repositoryName) {
    definition {
      cpsScm {
        scm {
          git {
            branch("master")
            remote {
              url(repo.repositorySsh)
              credentials("bitbucketKey")
            }
          }
        }
        scriptPath('Jenkinsfile')
        lightweight(true)
      }
    }
  }
  //create pr job
  //create merge job

}
