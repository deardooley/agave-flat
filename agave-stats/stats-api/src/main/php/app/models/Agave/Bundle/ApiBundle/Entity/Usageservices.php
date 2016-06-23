<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Usageservices
 */
class Usageservices
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var string
     */
    private $servicekey;

    /**
     * @var string
     */
    private $description;


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
     * Set servicekey
     *
     * @param string $servicekey
     * @return Usageservices
     */
    public function setServicekey($servicekey)
    {
        $this->servicekey = $servicekey;

        return $this;
    }

    /**
     * Get servicekey
     *
     * @return string 
     */
    public function getServicekey()
    {
        return $this->servicekey;
    }

    /**
     * Set description
     *
     * @param string $description
     * @return Usageservices
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
}
