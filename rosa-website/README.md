Using the following GWT versions.

 * GWT version 2.7.0
 * gwt-maven-plugin version 2.7.0

For some reason, the test web application requires the module _gwt-dev_ in order to run properly. Without this
dependency, an error occurs when using the RPC services in the -core module. This dependency should not be here and
needs to be addressed.