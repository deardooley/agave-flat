<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * AuthenticationTokens
 */
class AuthenticationTokens
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var \DateTime
     */
    private $createdAt;

    /**
     * @var string
     */
    private $creator;

    /**
     * @var \DateTime
     */
    private $expiresAt;

    /**
     * @var string
     */
    private $internalUsername;

    /**
     * @var string
     */
    private $ipAddress;

    /**
     * @var \DateTime
     */
    private $renewedAt;

    /**
     * @var integer
     */
    private $remainingUses;

    /**
     * @var string
     */
    private $token;

    /**
     * @var string
     */
    private $username;


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
     * Set createdAt
     *
     * @param \DateTime $createdAt
     * @return AuthenticationTokens
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
     * Set creator
     *
     * @param string $creator
     * @return AuthenticationTokens
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
     * Set expiresAt
     *
     * @param \DateTime $expiresAt
     * @return AuthenticationTokens
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
     * Set internalUsername
     *
     * @param string $internalUsername
     * @return AuthenticationTokens
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
     * Set ipAddress
     *
     * @param string $ipAddress
     * @return AuthenticationTokens
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
     * Set renewedAt
     *
     * @param \DateTime $renewedAt
     * @return AuthenticationTokens
     */
    public function setRenewedAt($renewedAt)
    {
        $this->renewedAt = $renewedAt;

        return $this;
    }

    /**
     * Get renewedAt
     *
     * @return \DateTime 
     */
    public function getRenewedAt()
    {
        return $this->renewedAt;
    }

    /**
     * Set remainingUses
     *
     * @param integer $remainingUses
     * @return AuthenticationTokens
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
     * Set token
     *
     * @param string $token
     * @return AuthenticationTokens
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
     * Set username
     *
     * @param string $username
     * @return AuthenticationTokens
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
}
