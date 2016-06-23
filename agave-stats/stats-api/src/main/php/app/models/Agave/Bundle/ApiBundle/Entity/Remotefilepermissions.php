<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Remotefilepermissions
 */
class Remotefilepermissions
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var string
     */
    private $internalUsername;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var integer
     */
    private $logicalFileId;

    /**
     * @var string
     */
    private $permission;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var string
     */
    private $username;

    /**
     * @var boolean
     */
    private $isRecursive;


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
     * Set created
     *
     * @param \DateTime $created
     * @return Remotefilepermissions
     */
    public function setCreated($created)
    {
        $this->created = $created;

        return $this;
    }

    /**
     * Get created
     *
     * @return \DateTime 
     */
    public function getCreated()
    {
        return $this->created;
    }

    /**
     * Set internalUsername
     *
     * @param string $internalUsername
     * @return Remotefilepermissions
     */
    public function setInternalUsername($internalUsername)
    {
        $this->internalUsername = $internalUsername;

        return $this;
    }

    /**
     * Get internalUsername
     *
     * @return string 
     */
    public function getInternalUsername()
    {
        return $this->internalUsername;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Remotefilepermissions
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
     * Set logicalFileId
     *
     * @param integer $logicalFileId
     * @return Remotefilepermissions
     */
    public function setLogicalFileId($logicalFileId)
    {
        $this->logicalFileId = $logicalFileId;

        return $this;
    }

    /**
     * Get logicalFileId
     *
     * @return integer 
     */
    public function getLogicalFileId()
    {
        return $this->logicalFileId;
    }

    /**
     * Set permission
     *
     * @param string $permission
     * @return Remotefilepermissions
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
     * Set tenantId
     *
     * @param string $tenantId
     * @return Remotefilepermissions
     */
    public function setTenantId($tenantId)
    {
        $this->tenantId = $tenantId;

        return $this;
    }

    /**
     * Get tenantId
     *
     * @return string 
     */
    public function getTenantId()
    {
        return $this->tenantId;
    }

    /**
     * Set username
     *
     * @param string $username
     * @return Remotefilepermissions
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
     * Set isRecursive
     *
     * @param boolean $isRecursive
     * @return Remotefilepermissions
     */
    public function setIsRecursive($isRecursive)
    {
        $this->isRecursive = $isRecursive;

        return $this;
    }

    /**
     * Get isRecursive
     *
     * @return boolean 
     */
    public function getIsRecursive()
    {
        return $this->isRecursive;
    }
}
