<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Storagesystems
 */
class Storagesystems
{
    /**
     * @var string
     */
    private $type;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $id;


    /**
     * Set type
     *
     * @param string $type
     * @return Storagesystems
     */
    public function setType($type)
    {
        $this->type = $type;

        return $this;
    }

    /**
     * Get type
     *
     * @return string 
     */
    public function getType()
    {
        return $this->type;
    }

    /**
     * Set id
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $id
     * @return Storagesystems
     */
    public function setId(\Agave\Bundle\ApiBundle\Entity\Systems $id = null)
    {
        $this->id = $id;

        return $this;
    }

    /**
     * Get id
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems 
     */
    public function getId()
    {
        return $this->id;
    }
}
