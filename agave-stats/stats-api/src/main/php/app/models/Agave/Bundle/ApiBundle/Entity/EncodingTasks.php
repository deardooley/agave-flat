<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * EncodingTasks
 */
class EncodingTasks
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var string
     */
    private $callbackKey;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var string
     */
    private $destPath;

    /**
     * @var string
     */
    private $sourcePath;

    /**
     * @var string
     */
    private $status;

    /**
     * @var string
     */
    private $transformName;

    /**
     * @var string
     */
    private $transformFilterName;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\LogicalFiles
     */
    private $logicalFile;

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
     * Set callbackKey
     *
     * @param string $callbackKey
     * @return EncodingTasks
     */
    public function setCallbackKey($callbackKey)
    {
        $this->callbackKey = $callbackKey;

        return $this;
    }

    /**
     * Get callbackKey
     *
     * @return string 
     */
    public function getCallbackKey()
    {
        return $this->callbackKey;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return EncodingTasks
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
     * Set destPath
     *
     * @param string $destPath
     * @return EncodingTasks
     */
    public function setDestPath($destPath)
    {
        $this->destPath = $destPath;

        return $this;
    }

    /**
     * Get destPath
     *
     * @return string 
     */
    public function getDestPath()
    {
        return $this->destPath;
    }

    /**
     * Set sourcePath
     *
     * @param string $sourcePath
     * @return EncodingTasks
     */
    public function setSourcePath($sourcePath)
    {
        $this->sourcePath = $sourcePath;

        return $this;
    }

    /**
     * Get sourcePath
     *
     * @return string 
     */
    public function getSourcePath()
    {
        return $this->sourcePath;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return EncodingTasks
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
     * Set transformName
     *
     * @param string $transformName
     * @return EncodingTasks
     */
    public function setTransformName($transformName)
    {
        $this->transformName = $transformName;

        return $this;
    }

    /**
     * Get transformName
     *
     * @return string 
     */
    public function getTransformName()
    {
        return $this->transformName;
    }

    /**
     * Set transformFilterName
     *
     * @param string $transformFilterName
     * @return EncodingTasks
     */
    public function setTransformFilterName($transformFilterName)
    {
        $this->transformFilterName = $transformFilterName;

        return $this;
    }

    /**
     * Get transformFilterName
     *
     * @return string 
     */
    public function getTransformFilterName()
    {
        return $this->transformFilterName;
    }

    /**
     * Set logicalFile
     *
     * @param \Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalFile
     * @return EncodingTasks
     */
    public function setLogicalFile(\Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalFile = null)
    {
        $this->logicalFile = $logicalFile;

        return $this;
    }

    /**
     * Get logicalFile
     *
     * @return \Agave\Bundle\ApiBundle\Entity\LogicalFiles 
     */
    public function getLogicalFile()
    {
        return $this->logicalFile;
    }

    /**
     * Set system
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $system
     * @return EncodingTasks
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
