<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * MetadataPermissions
 */
class MetadataPermissions
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
     * @var string
     */
    private $uuid;

    /**
     * @var string
     */
    private $tenantId;


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
     * @return MetadataPermissions
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
     * @return MetadataPermissions
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
     * @return MetadataPermissions
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
     * Set uuid
     *
     * @param string $uuid
     * @return MetadataPermissions
     */
    public function setUuid($uuid)
    {
        $this->uuid = $uuid;

        return $this;
    }

    /**
     * Get uuid
     *
     * @return string 
     */
    public function getUuid()
    {
        return $this->uuid;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return MetadataPermissions
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
}
