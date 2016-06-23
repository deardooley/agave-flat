<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * MetadataSchemaPermissions
 */
class MetadataSchemaPermissions
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
    private $schemaId;

    /**
     * @var string
     */
    private $username;

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
     * @return MetadataSchemaPermissions
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
     * @return MetadataSchemaPermissions
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
     * Set schemaId
     *
     * @param string $schemaId
     * @return MetadataSchemaPermissions
     */
    public function setSchemaId($schemaId)
    {
        $this->schemaId = $schemaId;

        return $this;
    }

    /**
     * Get schemaId
     *
     * @return string 
     */
    public function getSchemaId()
    {
        return $this->schemaId;
    }

    /**
     * Set username
     *
     * @param string $username
     * @return MetadataSchemaPermissions
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
     * Set tenantId
     *
     * @param string $tenantId
     * @return MetadataSchemaPermissions
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
