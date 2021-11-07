/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.experiments.radice.comparison.compat;

import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

/**
 * A Time-Shared VM Scheduler which allows over-subscription. In other
 * words, the scheduler still enables allocating into a Host, VMs which require more CPU
 * MIPS than there is available. If the Host has at least the number of PEs a VM
 * requires, the VM will be allowed to run into it.
 *
 * <p>The scheduler doesn't in fact allocates more MIPS for Virtual PEs (vPEs)
 * than there is in the physical PEs. It just reduces the allocated
 * amount according to the available MIPS.
 * This is an over-subscription, resulting in performance degradation
 * because less MIPS may be allocated than the required by a VM.
 * </p>
 *
 * @author Anton Beloglazov
 * @author Rodrigo N. Calheiros
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 3.0
 */
public class VmSchedulerTimeSharedOverSubscription extends VmSchedulerTimeShared {
    /**
     * Creates a time-shared over-subscription VM scheduler.
     */
    public VmSchedulerTimeSharedOverSubscription() {
        this(DEF_VM_MIGRATION_CPU_OVERHEAD);
    }

    /**
     * Creates a time-shared over-subscription VM scheduler, defining a CPU overhead for VM migration.
     *
     * @param vmMigrationCpuOverhead the percentage of Host's CPU usage increase when a
     * VM is migrating in or out of the Host. The value is in scale from 0 to 1 (where 1 is 100%).
     */
    public VmSchedulerTimeSharedOverSubscription(final double vmMigrationCpuOverhead){
        super(vmMigrationCpuOverhead);
    }

    /**
     * Checks if a list of MIPS requested by a VM is allowed to be allocated or not.
     * When there isn't the amount of requested MIPS available, this {@code VmScheduler}
     * allows to allocate what is available for the requesting VM,
     * allocating less that is requested.
     *
     * <p>This way, the only situation when it will not allow
     * the allocation of MIPS for a VM is when the number of PEs
     * required is greater than the total number of physical PEs.
     * Even when there is not available MIPS at all, it allows
     * the allocation of MIPS for the VM by reducing the allocation
     * of other VMs.</p>
     *
     * @param vm {@inheritDoc}
     * @param requestedMips {@inheritDoc}
     * @return true if the requested MIPS List is allowed to be allocated to the VM, false otherwise
     * @see VmSchedulerTimeShared#allocateMipsShareForVm(Vm, MipsShare)
     */
    @Override
    protected boolean isSuitableForVmInternal(final Vm vm, final MipsShare requestedMips){
        return getHost().getWorkingPesNumber() >= requestedMips.pes();
    }

    @Override
    protected void allocateMipsShareForVm(final Vm vm, final MipsShare requestedMipsReduced) {
        if(requestedMipsReduced.isEmpty()){
            return;
        }

        final double totalRequestedMips = requestedMipsReduced.totalMips();
        if (getTotalAvailableMips() >= totalRequestedMips) {
            super.allocateMipsShareForVm(vm, requestedMipsReduced);
            return;
        }

        ((VmSimple)vm).setAllocatedMips(requestedMipsReduced);
    }
}
