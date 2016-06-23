<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * SoftwarePermissions
 */
class SoftwarePermissions
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var string
     */
    private $permission;

    /**
     * @var string
     */
    private $username;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Softwares
     */
    private $software;


    /**
     * Get id
     *
     * @return integer 
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return SoftwarePermissions
     */
    public function setLastUpdated($lastUpdated)
    {
        $this->lastUpdated = $lastUpdated;

        return $this;
    }

    /**
     * Get lastUpdated
     *
     * @return \DateTime 
     */
    public function getLastUpdated()
    {
        return $this->lastUpdated;
    }

    /**
     * Set permission
     *
     * @param string $permission
     * @return SoftwarePermissions
     */
    public function setPermission($permission)
    {
        $this->permission = $permission;

        return $this;
    }

    /**
     * Get permission
     *
     * @return string 
     */
    public function getPermission()
    {
        return $this->permission;
    }

    /**
     * Set username
     *
     * @param string $username
     * @return SoftwarePermissions
     */
    public function setUsername($username)
    {
        $this->username = $username;

        return $this;
    }

    /**
     * Get username
     *
     * @return string 
     */
    public function getUsername()
    {
        return $this->username;
    }

    /**
     * Set software
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Softwares $software
     * @return SoftwarePermissions
     */
    public function setSoftware(\Agave\Bundle\ApiBundle\Entity\Softwares $software = null)
    {
        $this->software = $software;

        return $this;
    }

    /**
     * Get software
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Softwares 
     */
    public function getSoftware()
    {
        return $this->software;
    }
}
