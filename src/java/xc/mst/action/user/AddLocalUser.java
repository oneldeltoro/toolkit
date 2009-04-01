
/**
  * Copyright (c) 2009 University of Rochester
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the MIT/X11 license. The text of the
  * license can be found at http://www.opensource.org/licenses/mit-license.php and copy of the license can be found on the project
  * website http://www.extensiblecatalog.org/.
  *
  */

package xc.mst.action.user;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import xc.mst.bo.user.Group;
import xc.mst.bo.user.Server;
import xc.mst.bo.user.User;
import xc.mst.constants.Constants;
import xc.mst.manager.user.DefaultGroupService;
import xc.mst.manager.user.DefaultServerService;
import xc.mst.manager.user.DefaultUserGroupUtilService;
import xc.mst.manager.user.DefaultUserService;
import xc.mst.manager.user.GroupService;
import xc.mst.manager.user.UserGroupUtilService;
import xc.mst.manager.user.UserService;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This action method is used to add a new local user
 *
 * @author Tejaswi Haramurali
 */
public class AddLocalUser extends ActionSupport
{
    /** creates service object for users */
    private UserService userService = new DefaultUserService();

    /** creates service object for groups */
    private GroupService groupService = new DefaultGroupService();

    /**The email ID of the user */
    private String email;

    /** The password of the user */
    private String password;

    /** The username of the user */
    private String userName;

    /**The first Name of the user */
    private String firstName;
    
    /**The Last Name of the user */
    private String lastName;

    /**Comments that help the administrator recognise the new user */
    private String comments;

    /**The groups that have been assigned to the new user */
    private String[] groupsSelected;

    /**The list of all groups in the system */
    private List<Group> groupList;

    /**This User object is used to pre-fill JSP form fields */
    private User temporaryUser;

    /**Provides a list of selected group IDs which are used to pre-fill JSP form fields */
    private String[] selectedGroups;

     /** A reference to the logger for this class */
    static Logger log = Logger.getLogger(Constants.LOGGER_GENERAL);

	/** Error type */
	private String errorType; 
	

    /**
     * sets temporary user object
     * @param user temporary user object
     */
    public void setTemporaryUser(User user)
    {
        this.temporaryUser = user;
    }

    /**
     * returns temporary user object
     * @return temporary user
     */
    public User getTemporaryUser()
    {
        return temporaryUser;
    }

    /**
     * sets the user name for the local user
     * @param userName user Name
     */
    public void setUserName(String userName)
    {
        this.userName = userName.trim();
    }

    /**
     * returns the local name for the user
     * @return user name
     */
    public String getUserName()
    {
        return this.userName;
    }
    /**
     * assigns the list of groups that a user can belong to
     * @param groupList list of groups
     */
    public void setGroupList(List<Group> groupList)
    {
        this.groupList = groupList;
    }

    /**
     * returns a list of groups that a user can belong to
     * @return list of groups
     */
    public List<Group> getGroupList()
    {
        return groupList;
    }

    /**
     * sets the email ID of the user
     * @param email email ID
     */
    public void setEmail(String email)
    {
        this.email = email.trim();
    }

    /**
     * returns the email ID of the user
     * @return email ID
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * sets the password of the user
     * @param password The password to be assigned
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * returns the password of the user
     * @return user's password
     */
    public String getPassword()
    {
        return password;
    }

     /**
     * sets the list of groups that the user has been assigned
     * @param selectedGroupList list of selected groups
     */
    public void setGroupsSelected(String[] groupsSelected)
    {
        this.groupsSelected = groupsSelected;
    }

    /**
     * returns the list of groups that have been assigned to the user
     * @return list of selected groups
     */
    public String[] getGroupsSelected()
    {
        return groupsSelected;
    }

    /**
     * sets the list of groups that the user has been assigned (used to pre-fill JSP form fields)
     * @param selectedGroupList list of selected groups
     */
    public void setSelectedGroups(String[] selectedGroups)
    {
        this.selectedGroups = selectedGroups;
    }

    /**
     * returns the list of groups that have been assigned to the user (used to pre-fill JSP form fields)
     * @return list of selected groups
     */
    public String[] getSelectedGroups()
    {
        return selectedGroups;
    }
     /**
     * Overrides default implementation to view the add local user page.
     * @return {@link #SUCCESS}
     */
    @Override
    public String execute()
    {
        try
        {
            setGroupList(groupService.getAllGroups());
            return SUCCESS;
        }
        catch(Exception e)
        {
            log.debug(e);
            e.printStackTrace();
            this.addFieldError("addLocalUserError","Error : Groups List not displayed correctly");
            errorType = "error";
            return ERROR;
        }
    }

    /**
     * The action method that actually does the task of adding a new local user to the system
     * @return returns status of the add operation
     */
    public String addLocalUser()
    {
        try
        {           
            User user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            user.setAccountCreated(new Date());
            user.setFailedLoginAttempts(0);
            user.setUsername(userName);
            user.setLastLogin(new Date());
            Server localServer = new DefaultServerService().getServerByName("Local");
            user.setServer(localServer);

            User similarUserName = userService.getUserByUserName(user.getUsername(), localServer);
            User similarEmail = userService.getUserByEmail(email, localServer);
            if(similarUserName!=null)
            {
                if(similarUserName.getServer().getName().equalsIgnoreCase("Local"))
                {
                    this.addFieldError("addLocalUserError","Error : Username already exists");
                    errorType = "error";
                    setGroupList(groupService.getAllGroups());
                    setTemporaryUser(user);
                    setSelectedGroups(groupsSelected);
                    return INPUT;
                }
            }
            if(similarEmail!=null)
            {
                if(similarEmail.getServer().getName().equalsIgnoreCase("Local"))
                {
                    this.addFieldError("addLocalUserError","Error : Email ID already exists");
                    errorType = "error";
                    setGroupList(groupService.getAllGroups());
                    setTemporaryUser(user);
                    setSelectedGroups(groupsSelected);
                    return INPUT;
                }
            }
          

            for(int i=0;i<groupsSelected.length;i++)
            {
                Group tempGroup = groupService.getGroupById(Integer.parseInt(groupsSelected[i]));
                user.addGroup(tempGroup);
            }
            userService.insertUser(user);
            return SUCCESS;
        }
        catch(Exception e)
        {
            log.debug(e);
            e.printStackTrace();
            this.addFieldError("addLocalUserError","Error : User not Added correctly");
            errorType = "error";
            return ERROR;
        }

    }

	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName.trim();
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName.trim();
	}
}
