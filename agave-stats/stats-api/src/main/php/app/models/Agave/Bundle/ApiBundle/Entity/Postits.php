<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Postits
 * @ORM\Entity
 * @ORM\Table(name="postits")
 */
class Postits
{
  /**
  * @ORM\Id
  * @ORM\GeneratedValue
  * @ORM\Column(type="integer")
  */
  private $id;

    /**
     * @var string
     */
    private $targetUrl;

    /**
     * @var string
     */
    private $targetMethod;

    /**
     * @var string
     */
    private $postitKey;

    /**
     * @var string
     */
    private $creator;

    /**
     * @var string
     */
    private $token;

    /**
     * @var string
     */
    private $ipAddress;

    /**
     * @var \DateTime
     */
    private $createdAt;

    /**
     * @var \DateTime
     */
    private $expiresAt;

    /**
     * @var integer
     */
    private $remainingUses;

    /**
     * @var string
     */
    private $internalUsername;

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
     * Set targetUrl
     *
     * @param string $targetUrl
     * @return Postits
     */
    public function setTargetUrl($targetUrl)
    {
        $this->targetUrl = $targetUrl;

        return $this;
    }

    /**
     * Get targetUrl
     *
     * @return string
     */
    public function getTargetUrl()
    {
        return $this->targetUrl;
    }

    /**
     * Set targetMethod
     *
     * @param string $targetMethod
     * @return Postits
     */
    public function setTargetMethod($targetMethod)
    {
        $this->targetMethod = $targetMethod;

        return $this;
    }

    /**
     * Get targetMethod
     *
     * @return string
     */
    public function getTargetMethod()
    {
        return $this->targetMethod;
    }

    /**
     * Set postitKey
     *
     * @param string $postitKey
     * @return Postits
     */
    public function setPostitKey($postitKey)
    {
        $this->postitKey = $postitKey;

        return $this;
    }

    /**
     * Get postitKey
     *
     * @return string
     */
    public function getPostitKey()
    {
        return $this->postitKey;
    }

    /**
     * Set creator
     *
     * @param string $creator
     * @return Postits
     */
    public function setCreator($creator)
    {
        $this->creator = $creator;

        return $this;
    }

    /**
     * Get creator
     *
     * @return string
     */
    public function getCreator()
    {
        return $this->creator;
    }

    /**
     * Set token
     *
     * @param string $token
     * @return Postits
     */
    public function setToken($token)
    {
        $this->token = $token;

        return $this;
    }

    /**
     * Get token
     *
     * @return string
     */
    public function getToken()
    {
        return $this->token;
    }

    /**
     * Set ipAddress
     *
     * @param string $ipAddress
     * @return Postits
     */
    public function setIpAddress($ipAddress)
    {
        $this->ipAddress = $ipAddress;

        return $this;
    }

    /**
     * Get ipAddress
     *
     * @return string
     */
    public function getIpAddress()
    {
        return $this->ipAddress;
    }

    /**
     * Set createdAt
     *
     * @param \DateTime $createdAt
     * @return Postits
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
     * Set expiresAt
     *
     * @param \DateTime $expiresAt
     * @return Postits
     */
    public function setExpiresAt($expiresAt)
    {
        $this->expiresAt = $expiresAt;

        return $this;
    }

    /**
     * Get expiresAt
     *
     * @return \DateTime
     */
    public function getExpiresAt()
    {
        return $this->expiresAt;
    }

    /**
     * Set remainingUses
     *
     * @param integer $remainingUses
     * @return Postits
     */
    public function setRemainingUses($remainingUses)
    {
        $this->remainingUses = $remainingUses;

        return $this;
    }

    /**
     * Get remainingUses
     *
     * @return integer
     */
    public function getRemainingUses()
    {
        return $this->remainingUses;
    }

    /**
     * Set internalUsername
     *
     * @param string $internalUsername
     * @return Postits
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
     * Set tenantId
     *
     * @param string $tenantId
     * @return Postits
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
