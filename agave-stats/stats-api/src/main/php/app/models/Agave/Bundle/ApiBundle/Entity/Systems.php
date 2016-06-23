<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Systems
 */
class Systems
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var boolean
     */
    private $available;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var string
     */
    private $description;

    /**
     * @var boolean
     */
    private $globalDefault;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var string
     */
    private $name;

    /**
     * @var string
     */
    private $owner;

    /**
     * @var boolean
     */
    private $publiclyAvailable;

    /**
     * @var integer
     */
    private $revision;

    /**
     * @var string
     */
    private $site;

    /**
     * @var string
     */
    private $status;

    /**
     * @var string
     */
    private $systemId;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var string
     */
    private $type;

    /**
     * @var string
     */
    private $uuid;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Storageconfigs
     */
    private $storageConfig;


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
     * Set available
     *
     * @param boolean $available
     * @return Systems
     */
    public function setAvailable($available)
    {
        $this->available = $available;

        return $this;
    }

    /**
     * Get available
     *
     * @return boolean 
     */
    public function getAvailable()
    {
        return $this->available;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Systems
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
     * Set description
     *
     * @param string $description
     * @return Systems
     */
    public function setDescription($description)
    {
        $this->description = $description;

        return $this;
    }

    /**
     * Get description
     *
     * @return string 
     */
    public function getDescription()
    {
        return $this->description;
    }

    /**
     * Set globalDefault
     *
     * @param boolean $globalDefault
     * @return Systems
     */
    public function setGlobalDefault($globalDefault)
    {
        $this->globalDefault = $globalDefault;

        return $this;
    }

    /**
     * Get globalDefault
     *
     * @return boolean 
     */
    public function getGlobalDefault()
    {
        return $this->globalDefault;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Systems
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
     * Set name
     *
     * @param string $name
     * @return Systems
     */
    public function setName($name)
    {
        $this->name = $name;

        return $this;
    }

    /**
     * Get name
     *
     * @return string 
     */
    public function getName()
    {
        return $this->name;
    }

    /**
     * Set owner
     *
     * @param string $owner
     * @return Systems
     */
    public function setOwner($owner)
    {
        $this->owner = $owner;

        return $this;
    }

    /**
     * Get owner
     *
     * @return string 
     */
    public function getOwner()
    {
        return $this->owner;
    }

    /**
     * Set publiclyAvailable
     *
     * @param boolean $publiclyAvailable
     * @return Systems
     */
    public function setPubliclyAvailable($publiclyAvailable)
    {
        $this->publiclyAvailable = $publiclyAvailable;

        return $this;
    }

    /**
     * Get publiclyAvailable
     *
     * @return boolean 
     */
    public function getPubliclyAvailable()
    {
        return $this->publiclyAvailable;
    }

    /**
     * Set revision
     *
     * @param integer $revision
     * @return Systems
     */
    public function setRevision($revision)
    {
        $this->revision = $revision;

        return $this;
    }

    /**
     * Get revision
     *
     * @return integer 
     */
    public function getRevision()
    {
        return $this->revision;
    }

    /**
     * Set site
     *
     * @param string $site
     * @return Systems
     */
    public function setSite($site)
    {
        $this->site = $site;

        return $this;
    }

    /**
     * Get site
     *
     * @return string 
     */
    public function getSite()
    {
        return $this->site;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return Systems
     */
    public function setStatus($status)
    {
        $this->status = $status;

        return $this;
    }

    /**
     * Get status
     *
     * @return string 
     */
    public function getStatus()
    {
        return $this->status;
    }

    /**
     * Set systemId
     *
     * @param string $systemId
     * @return Systems
     */
    public function setSystemId($systemId)
    {
        $this->systemId = $systemId;

        return $this;
    }

    /**
     * Get systemId
     *
     * @return string 
     */
    public function getSystemId()
    {
        return $this->systemId;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return Systems
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
     * Set type
     *
     * @param string $type
     * @return Systems
     */
    public function setType($type)
    {
        $this->type = $type;

        return $this;
    }

    /**
     * Get type
     *
     * @return string 
     */
    public function getType()
    {
        return $this->type;
    }

    /**
     * Set uuid
     *
     * @param string $uuid
     * @return Systems
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
     * Set storageConfig
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Storageconfigs $storageConfig
     * @return Systems
     */
    public function setStorageConfig(\Agave\Bundle\ApiBundle\Entity\Storageconfigs $storageConfig = null)
    {
        $this->storageConfig = $storageConfig;

        return $this;
    }

    /**
     * Get storageConfig
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Storageconfigs 
     */
    public function getStorageConfig()
    {
        return $this->storageConfig;
    }
}
