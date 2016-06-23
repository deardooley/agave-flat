<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Storageconfigs
 */
class Storageconfigs
{
    /**
     * @var string
     */
    private $homeDir;

    /**
     * @var boolean
     */
    private $mirrorPermissions;

    /**
     * @var string
     */
    private $protocol;

    /**
     * @var string
     */
    private $resource;

    /**
     * @var string
     */
    private $rootDir;

    /**
     * @var string
     */
    private $zone;

    /**
     * @var string
     */
    private $publicAppsDir;

    /**
     * @var string
     */
    private $container;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Remoteconfigs
     */
    private $id;


    /**
     * Set homeDir
     *
     * @param string $homeDir
     * @return Storageconfigs
     */
    public function setHomeDir($homeDir)
    {
        $this->homeDir = $homeDir;

        return $this;
    }

    /**
     * Get homeDir
     *
     * @return string 
     */
    public function getHomeDir()
    {
        return $this->homeDir;
    }

    /**
     * Set mirrorPermissions
     *
     * @param boolean $mirrorPermissions
     * @return Storageconfigs
     */
    public function setMirrorPermissions($mirrorPermissions)
    {
        $this->mirrorPermissions = $mirrorPermissions;

        return $this;
    }

    /**
     * Get mirrorPermissions
     *
     * @return boolean 
     */
    public function getMirrorPermissions()
    {
        return $this->mirrorPermissions;
    }

    /**
     * Set protocol
     *
     * @param string $protocol
     * @return Storageconfigs
     */
    public function setProtocol($protocol)
    {
        $this->protocol = $protocol;

        return $this;
    }

    /**
     * Get protocol
     *
     * @return string 
     */
    public function getProtocol()
    {
        return $this->protocol;
    }

    /**
     * Set resource
     *
     * @param string $resource
     * @return Storageconfigs
     */
    public function setResource($resource)
    {
        $this->resource = $resource;

        return $this;
    }

    /**
     * Get resource
     *
     * @return string 
     */
    public function getResource()
    {
        return $this->resource;
    }

    /**
     * Set rootDir
     *
     * @param string $rootDir
     * @return Storageconfigs
     */
    public function setRootDir($rootDir)
    {
        $this->rootDir = $rootDir;

        return $this;
    }

    /**
     * Get rootDir
     *
     * @return string 
     */
    public function getRootDir()
    {
        return $this->rootDir;
    }

    /**
     * Set zone
     *
     * @param string $zone
     * @return Storageconfigs
     */
    public function setZone($zone)
    {
        $this->zone = $zone;

        return $this;
    }

    /**
     * Get zone
     *
     * @return string 
     */
    public function getZone()
    {
        return $this->zone;
    }

    /**
     * Set publicAppsDir
     *
     * @param string $publicAppsDir
     * @return Storageconfigs
     */
    public function setPublicAppsDir($publicAppsDir)
    {
        $this->publicAppsDir = $publicAppsDir;

        return $this;
    }

    /**
     * Get publicAppsDir
     *
     * @return string 
     */
    public function getPublicAppsDir()
    {
        return $this->publicAppsDir;
    }

    /**
     * Set container
     *
     * @param string $container
     * @return Storageconfigs
     */
    public function setContainer($container)
    {
        $this->container = $container;

        return $this;
    }

    /**
     * Get container
     *
     * @return string 
     */
    public function getContainer()
    {
        return $this->container;
    }

    /**
     * Set id
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Remoteconfigs $id
     * @return Storageconfigs
     */
    public function setId(\Agave\Bundle\ApiBundle\Entity\Remoteconfigs $id = null)
    {
        $this->id = $id;

        return $this;
    }

    /**
     * Get id
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Remoteconfigs 
     */
    public function getId()
    {
        return $this->id;
    }
}
