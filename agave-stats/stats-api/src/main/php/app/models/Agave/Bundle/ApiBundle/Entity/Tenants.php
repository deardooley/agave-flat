<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Tenants
 * @ORM\Entity
 * @ORM\Table(name="`tenants`")
 */
class Tenants
{
    /**
    * @ORM\Id
    * @ORM\GeneratedValue
    * @ORM\Column(type="integer")
    */
    private $id;

    /**
     * @var string
     * @ORM\Column(type="string", name="name")
     */
    private $name;

    /**
     * @var string
     * @ORM\Column(type="string", name="base_url")
     */
    private $baseUrl;

    /**
     * @var string
     * @ORM\Column(type="string", name="contact_email")
     */
    private $contactEmail;

    /**
     * @var string
     * @ORM\Column(type="string", name="contact_name")
     */
    private $contactName;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="created")
     */
    private $created;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="lastUpdated")
     */
    private $lastUpdated;

    /**
     * @var string
     * @ORM\Column(type="string", name="status")
     */
    private $status;

    /**
     * @var string
     * @ORM\Column(type="string", name="tenant_id")
     */
    private $tenantId;

    /**
     * @var string
     * @ORM\Column(type="string", name="uuid")
     */
    private $uuid;


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
     * Set name
     *
     * @param string $name
     * @return Tenants
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
     * Set baseUrl
     *
     * @param string $baseUrl
     * @return Tenants
     */
    public function setBaseUrl($baseUrl)
    {
        $this->baseUrl = $baseUrl;

        return $this;
    }

    /**
     * Get baseUrl
     *
     * @return string
     */
    public function getBaseUrl()
    {
        return $this->baseUrl;
    }

    /**
     * Set contactEmail
     *
     * @param string $contactEmail
     * @return Tenants
     */
    public function setContactEmail($contactEmail)
    {
        $this->contactEmail = $contactEmail;

        return $this;
    }

    /**
     * Get contactEmail
     *
     * @return string
     */
    public function getContactEmail()
    {
        return $this->contactEmail;
    }

    /**
     * Set contactName
     *
     * @param string $contactName
     * @return Tenants
     */
    public function setContactName($contactName)
    {
        $this->contactName = $contactName;

        return $this;
    }

    /**
     * Get contactName
     *
     * @return string
     */
    public function getContactName()
    {
        return $this->contactName;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Tenants
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
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Tenants
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
     * Set status
     *
     * @param string $status
     * @return Tenants
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
     * Set tenantId
     *
     * @param string $tenantId
     * @return Tenants
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
     * Set uuid
     *
     * @param string $uuid
     * @return Tenants
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
}
