<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * LogicalFiles
 */
class LogicalFiles
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
     * @var string
     */
    private $name;

    /**
     * @var string
     */
    private $nativeFormat;

    /**
     * @var string
     */
    private $owner;

    /**
     * @var string
     */
    private $path;

    /**
     * @var string
     */
    private $source;

    /**
     * @var string
     */
    private $status;

    /**
     * @var string
     */
    private $uuid;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $system;


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
     * @return LogicalFiles
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
     * @return LogicalFiles
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
     * @return LogicalFiles
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
     * @return LogicalFiles
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
     * Set nativeFormat
     *
     * @param string $nativeFormat
     * @return LogicalFiles
     */
    public function setNativeFormat($nativeFormat)
    {
        $this->nativeFormat = $nativeFormat;

        return $this;
    }

    /**
     * Get nativeFormat
     *
     * @return string 
     */
    public function getNativeFormat()
    {
        return $this->nativeFormat;
    }

    /**
     * Set owner
     *
     * @param string $owner
     * @return LogicalFiles
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
     * Set path
     *
     * @param string $path
     * @return LogicalFiles
     */
    public function setPath($path)
    {
        $this->path = $path;

        return $this;
    }

    /**
     * Get path
     *
     * @return string 
     */
    public function getPath()
    {
        return $this->path;
    }

    /**
     * Set source
     *
     * @param string $source
     * @return LogicalFiles
     */
    public function setSource($source)
    {
        $this->source = $source;

        return $this;
    }

    /**
     * Get source
     *
     * @return string 
     */
    public function getSource()
    {
        return $this->source;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return LogicalFiles
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
     * Set uuid
     *
     * @param string $uuid
     * @return LogicalFiles
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
     * @return LogicalFiles
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
     * Set system
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $system
     * @return LogicalFiles
     */
    public function setSystem(\Agave\Bundle\ApiBundle\Entity\Systems $system = null)
    {
        $this->system = $system;

        return $this;
    }

    /**
     * Get system
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems 
     */
    public function getSystem()
    {
        return $this->system;
    }
}
