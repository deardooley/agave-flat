<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * DecodingTasks
 */
class DecodingTasks
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
    private $currentFilter;

    /**
     * @var string
     */
    private $destPath;

    /**
     * @var string
     */
    private $destTransform;

    /**
     * @var string
     */
    private $destinationUri;

    /**
     * @var string
     */
    private $sourcePath;

    /**
     * @var string
     */
    private $srcTransform;

    /**
     * @var string
     */
    private $status;

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
     * @return DecodingTasks
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
     * @return DecodingTasks
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
     * Set currentFilter
     *
     * @param string $currentFilter
     * @return DecodingTasks
     */
    public function setCurrentFilter($currentFilter)
    {
        $this->currentFilter = $currentFilter;

        return $this;
    }

    /**
     * Get currentFilter
     *
     * @return string 
     */
    public function getCurrentFilter()
    {
        return $this->currentFilter;
    }

    /**
     * Set destPath
     *
     * @param string $destPath
     * @return DecodingTasks
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
     * Set destTransform
     *
     * @param string $destTransform
     * @return DecodingTasks
     */
    public function setDestTransform($destTransform)
    {
        $this->destTransform = $destTransform;

        return $this;
    }

    /**
     * Get destTransform
     *
     * @return string 
     */
    public function getDestTransform()
    {
        return $this->destTransform;
    }

    /**
     * Set destinationUri
     *
     * @param string $destinationUri
     * @return DecodingTasks
     */
    public function setDestinationUri($destinationUri)
    {
        $this->destinationUri = $destinationUri;

        return $this;
    }

    /**
     * Get destinationUri
     *
     * @return string 
     */
    public function getDestinationUri()
    {
        return $this->destinationUri;
    }

    /**
     * Set sourcePath
     *
     * @param string $sourcePath
     * @return DecodingTasks
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
     * Set srcTransform
     *
     * @param string $srcTransform
     * @return DecodingTasks
     */
    public function setSrcTransform($srcTransform)
    {
        $this->srcTransform = $srcTransform;

        return $this;
    }

    /**
     * Get srcTransform
     *
     * @return string 
     */
    public function getSrcTransform()
    {
        return $this->srcTransform;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return DecodingTasks
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
     * Set logicalFile
     *
     * @param \Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalFile
     * @return DecodingTasks
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
     * @return DecodingTasks
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
