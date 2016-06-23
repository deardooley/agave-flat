<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Authconfigs
 */
class Authconfigs
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
    private $credential;

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
    private $password;

    /**
     * @var boolean
     */
    private $systemDefault;

    /**
     * @var string
     */
    private $loginCredentialType;

    /**
     * @var string
     */
    private $username;

    /**
     * @var string
     */
    private $privateKey;

    /**
     * @var string
     */
    private $publicKey;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Remoteconfigs
     */
    private $remoteConfig;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Credentialservers
     */
    private $authenticationSystem;


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
     * @return Authconfigs
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
     * Set credential
     *
     * @param string $credential
     * @return Authconfigs
     */
    public function setCredential($credential)
    {
        $this->credential = $credential;

        return $this;
    }

    /**
     * Get credential
     *
     * @return string 
     */
    public function getCredential()
    {
        return $this->credential;
    }

    /**
     * Set internalUsername
     *
     * @param string $internalUsername
     * @return Authconfigs
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
     * @return Authconfigs
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
     * Set password
     *
     * @param string $password
     * @return Authconfigs
     */
    public function setPassword($password)
    {
        $this->password = $password;

        return $this;
    }

    /**
     * Get password
     *
     * @return string 
     */
    public function getPassword()
    {
        return $this->password;
    }

    /**
     * Set systemDefault
     *
     * @param boolean $systemDefault
     * @return Authconfigs
     */
    public function setSystemDefault($systemDefault)
    {
        $this->systemDefault = $systemDefault;

        return $this;
    }

    /**
     * Get systemDefault
     *
     * @return boolean 
     */
    public function getSystemDefault()
    {
        return $this->systemDefault;
    }

    /**
     * Set loginCredentialType
     *
     * @param string $loginCredentialType
     * @return Authconfigs
     */
    public function setLoginCredentialType($loginCredentialType)
    {
        $this->loginCredentialType = $loginCredentialType;

        return $this;
    }

    /**
     * Get loginCredentialType
     *
     * @return string 
     */
    public function getLoginCredentialType()
    {
        return $this->loginCredentialType;
    }

    /**
     * Set username
     *
     * @param string $username
     * @return Authconfigs
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
     * Set privateKey
     *
     * @param string $privateKey
     * @return Authconfigs
     */
    public function setPrivateKey($privateKey)
    {
        $this->privateKey = $privateKey;

        return $this;
    }

    /**
     * Get privateKey
     *
     * @return string 
     */
    public function getPrivateKey()
    {
        return $this->privateKey;
    }

    /**
     * Set publicKey
     *
     * @param string $publicKey
     * @return Authconfigs
     */
    public function setPublicKey($publicKey)
    {
        $this->publicKey = $publicKey;

        return $this;
    }

    /**
     * Get publicKey
     *
     * @return string 
     */
    public function getPublicKey()
    {
        return $this->publicKey;
    }

    /**
     * Set remoteConfig
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Remoteconfigs $remoteConfig
     * @return Authconfigs
     */
    public function setRemoteConfig(\Agave\Bundle\ApiBundle\Entity\Remoteconfigs $remoteConfig = null)
    {
        $this->remoteConfig = $remoteConfig;

        return $this;
    }

    /**
     * Get remoteConfig
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Remoteconfigs 
     */
    public function getRemoteConfig()
    {
        return $this->remoteConfig;
    }

    /**
     * Set authenticationSystem
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Credentialservers $authenticationSystem
     * @return Authconfigs
     */
    public function setAuthenticationSystem(\Agave\Bundle\ApiBundle\Entity\Credentialservers $authenticationSystem = null)
    {
        $this->authenticationSystem = $authenticationSystem;

        return $this;
    }

    /**
     * Get authenticationSystem
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Credentialservers 
     */
    public function getAuthenticationSystem()
    {
        return $this->authenticationSystem;
    }
}
