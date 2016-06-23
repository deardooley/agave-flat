<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Usage
 * @ORM\Entity
 * @ORM\Table(name="`Usage`")
 */
class Usage
{
    /**
    * @ORM\Id
    * @ORM\GeneratedValue
    * @ORM\Column(type="integer")
    */
    private $uid;

    /**
     * @var string
     * @ORM\Column(type="string", name="Username")
     */
    private $username;

    /**
     * @var string
     * @ORM\Column(type="string", name="ServiceKey")
     */
    private $serviceKey;

    /**
     * @var string
     * @ORM\Column(type="string", name="ActivityKey")
     */
    private $activityKey;

    /**
     * @var string
     * @ORM\Column(type="string", name="ActivityContext")
     */
    private $activityContext;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="CreatedAt")
     */
    private $createdAt;

    /**
     * @var string
     * @ORM\Column(type="string", name="CallingIP")
     */
    private $callingIp;

    /**
     * @var string
     * @ORM\Column(type="string", name="UserIP")
     */
    private $userIp;

    /**
     * @var string
     * @ORM\Column(type="string", name="ClientApplication")
     */
    private $clientApplication;

    /**
     * @var string
     * @ORM\Column(type="string", name="TenantId")
     */
    private $tenantId;

    /**
     * @var string
     * @ORM\Column(type="string", name="UserAgent")
     */
    private $userAgent;


    /**
     * Get uid
     *
     * @return integer
     */
    public function getUid()
    {
        return $this->uid;
    }

    /**
     * Set username
     *
     * @param string $username
     * @return Usage
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
     * Set serviceKey
     *
     * @param string $serviceKey
     * @return Usage
     */
    public function setServiceKey($serviceKey)
    {
        $this->serviceKey = $serviceKey;

        return $this;
    }

    /**
     * Get serviceKey
     *
     * @return string
     */
    public function getServiceKey()
    {
        return $this->serviceKey;
    }

    /**
     * Set activityKey
     *
     * @param string $activityKey
     * @return Usage
     */
    public function setActivityKey($activityKey)
    {
        $this->activityKey = $activityKey;

        return $this;
    }

    /**
     * Get activityKey
     *
     * @return string
     */
    public function getActivityKey()
    {
        return $this->activityKey;
    }

    /**
     * Set activityContext
     *
     * @param string $activityContext
     * @return Usage
     */
    public function setActivityContext($activityContext)
    {
        $this->activityContext = $activityContext;

        return $this;
    }

    /**
     * Get activityContext
     *
     * @return string
     */
    public function getActivityContext()
    {
        return $this->activityContext;
    }

    /**
     * Set createdAt
     *
     * @param \DateTime $createdAt
     * @return Usage
     */
    public function setCreatedAt($createdAt)
    {
        $this->createdAt = $createdAt;

        return $this;
    }

    /**
     * Get createdAt
     *
     * @return \DateTime
     */
    public function getCreatedAt()
    {
        return $this->createdAt;
    }

    /**
     * Set callingIp
     *
     * @param string $callingIp
     * @return Usage
     */
    public function setCallingIp($callingIp)
    {
        $this->callingIp = $callingIp;

        return $this;
    }

    /**
     * Get callingIp
     *
     * @return string
     */
    public function getCallingIp()
    {
        return $this->callingIp;
    }

    /**
     * Set userIp
     *
     * @param string $userIp
     * @return Usage
     */
    public function setUserIp($userIp)
    {
        $this->userIp = $userIp;

        return $this;
    }

    /**
     * Get userIp
     *
     * @return string
     */
    public function getUserIp()
    {
        return $this->userIp;
    }

    /**
     * Set clientApplication
     *
     * @param string $clientApplication
     * @return Usage
     */
    public function setClientApplication($clientApplication)
    {
        $this->clientApplication = $clientApplication;

        return $this;
    }

    /**
     * Get clientApplication
     *
     * @return string
     */
    public function getClientApplication()
    {
        return $this->clientApplication;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return Usage
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
     * Set userAgent
     *
     * @param string $userAgent
     * @return Usage
     */
    public function setUserAgent($userAgent)
    {
        $this->userAgent = $userAgent;

        return $this;
    }

    /**
     * Get userAgent
     *
     * @return string
     */
    public function getUserAgent()
    {
        return $this->userAgent;
    }
}
