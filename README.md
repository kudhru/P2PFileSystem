#Test Files
  - Representative directory files of 6MB and 10MB are provided in 'Test Files' folder.

# Server and Peer IP and port details
1. IP.properties file stores the list of ip addresses and port numbers for peers. If you want to make any change in the list of peers, make changes in this file.
2. Server.properties file stores the ip address and port number for server. If you want to make any change in the server ip address and port, make changes in this file.

# Make project
1. Run `make all` to compile the project.

# Stress Tests
1. Run `java TestMultipleUploadDownload` java file which instantiates multiple clients per peer and performs multiple downloads and uploads per peer.

# File Corruption
1. Run `java TestCorruptedFileDownload` java file which does the following:
    1. First the server is run. Then 6 peers are created.
    2. 5 files are created in each peer.
    3. Copy the files of Peer 0 into a dummy directory.
    4. Then corrupt the files of Peer 0.
    5. Peer 2 tries to download the files of Peer 0.
    6. Files cannot be downloaded since all the files of Peer 0 are corrupted.
    7. Now, copy all the files in the dummy dir to Peer 1 and update the server. 
    8. Again, Peer 2 tries to download the files of Peer 0.
    9. It is able to successfully download all the files of Peer 0 from Peer 1. 
    
# Interactive Terminal
1. First Start the server with `java Server`. This will start the server at ip address and port specified in SERVER.properties file.
2. Then, start multiple peers with the following commands:
    1. `java PeerClient <absolute_path_of_shared_directory> <Port_number>`.
    2. For example, if the absolute path of shared directory for Peer 1 is /Users/Dhruv/work/peer1/ and the port number is 6500, the command should be `java PeerClient /Users/Dhruv/work/peer1/ 6500`.
    3. Make sure that the port number specified in the above command is one of the ports present in IP.Properties.
    4. In the above manner, you can start as many peers as there are in the IP.Properties. 

5. Operations which can be performed at the server:
    1. `print`: prints the list of files along with the peers on which the file is present.
    2.  `stop`: Stops (exits) the server.
6. Operations which can be performed at any peer client:
    1. `find <file_name>`: finds out the list of peers which contain the file named `<file_name>`.
    2. `download <file_name>`: downloads the file named `<file_name>` if it exists in any of the peers.
    3. `updatelist`: forcefully send the updated list of files to the server. Server then updates its file server map.
    4. `stop`: Stop (exits) the server.   
