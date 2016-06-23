<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * SystemsSystemroles
 */
class SystemsSystemroles
{
    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $systems;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systemroles
     */
    private $roles;


    /**
     * Set systems
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $systems
     * @return SystemsSystemroles
     */
    public function setSystems(\Agave\Bundle\ApiBundle\Entity\Systems $systems = null)
    {
        $this->systems = $systems;

        return $this;
    }

    /**
     * Get systems
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems 
     */
    public function getSystems()
    {
        return $this->systems;
    }

    /**
     * Set roles
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systemroles $roles
     * @return SystemsSystemroles
     */
    public function setRoles(\Agave\Bundle\ApiBundle\Entity\Systemroles $roles = null)
    {
        $this->roles = $roles;

        return $this;
    }

    /**
     * Get roles
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systemroles 
     */
    public function getRoles()
    {
        return $this->roles;
    }
}
