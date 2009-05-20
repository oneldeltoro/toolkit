/**
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

package xc.mst.manager.user;


import java.util.List;
import xc.mst.bo.user.Server;
import xc.mst.dao.DataException;
import xc.mst.dao.DatabaseConfigException;
import xc.mst.dao.user.DefaultServerDAO;
import xc.mst.dao.user.ServerDAO;


/**
 * Service for Server
 *
 * @author Tejaswi Haramurali
 */
public class DefaultServerService implements ServerService {

    /** DAO object for servers */
    private ServerDAO serverDao = new DefaultServerDAO();

    /**
     * Return a server object based on the server ID
     *
     * @param serverId The ID of the server to be returned
     * @return
     * @throws DatabaseConfigException 
     */
    public Server getServerById(int serverId) throws DatabaseConfigException {

        return serverDao.getById(serverId);
    }

    /**
     * Return a server object based on the server Name
     *
     * @param serverName The name of the server to be returned
     * @return server object
     * @throws DatabaseConfigException 
     */
    public Server getServerByName(String serverName) throws DatabaseConfigException {

        return serverDao.getByName(serverName);
    }

    /**
     * Inserts a server object
     *
     * @param server The server object to be inserted
     */
    public void insertServer(Server server) throws DataException {

        serverDao.insert(server);
    }

    /**
     * Deletes a server
     *
     * @param server The server object to be deleted
     */
    public void deleteServer(Server server) throws DataException{
        serverDao.delete(server);
    }

    /**
     * Updates the details of a server
     * 
     * @param server The server whose details should be updated
     */
    public void updateServer(Server server) throws DataException{
        serverDao.update(server);
    }

    /**
     * Get all servers
     *
     * @return All servers
     * @throws DataException
     */
    public List<Server> getAll() throws DatabaseConfigException {
    	return serverDao.getAll();
    }
}
